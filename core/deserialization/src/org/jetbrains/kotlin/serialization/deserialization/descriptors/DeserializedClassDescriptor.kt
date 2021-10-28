/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.incremental.record
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinEnum
import org.jetbrains.kotlin.resolve.scopes.receivers.ContextClassReceiver
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.TypeRefinement
import org.jetbrains.kotlin.utils.addToStdlib.flatMapToNullable
import java.util.*

class DeserializedClassDescriptor(
    outerContext: DeserializationContext,
    val classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    val metadataVersion: BinaryVersion,
    private val sourceElement: SourceElement
) : AbstractClassDescriptor(
    outerContext.storageManager,
    nameResolver.getClassId(classProto.fqName).shortClassName
), DeserializedDescriptor {
    private val classId = nameResolver.getClassId(classProto.fqName)

    private val modality = ProtoEnumFlags.modality(Flags.MODALITY.get(classProto.flags))
    private val visibility = ProtoEnumFlags.descriptorVisibility(Flags.VISIBILITY.get(classProto.flags))
    private val kind = ProtoEnumFlags.classKind(Flags.CLASS_KIND.get(classProto.flags))

    val c = outerContext.childContext(
        this, classProto.typeParameterList, nameResolver, TypeTable(classProto.typeTable),
        VersionRequirementTable.create(classProto.versionRequirementTable), metadataVersion
    )

    private val staticScope = if (kind == ClassKind.ENUM_CLASS) StaticScopeForKotlinEnum(c.storageManager, this) else MemberScope.Empty
    private val typeConstructor = DeserializedClassTypeConstructor()

    private val memberScopeHolder =
        ScopesHolderForClass.create(this, c.storageManager, c.components.kotlinTypeChecker.kotlinTypeRefiner, this::DeserializedClassMemberScope)

    private val memberScope get() = memberScopeHolder.getScope(c.components.kotlinTypeChecker.kotlinTypeRefiner)
    private val enumEntries = if (kind == ClassKind.ENUM_CLASS) EnumEntryClassDescriptors() else null

    private val containingDeclaration = outerContext.containingDeclaration
    private val primaryConstructor = c.storageManager.createNullableLazyValue { computePrimaryConstructor() }
    private val constructors = c.storageManager.createLazyValue { computeConstructors() }
    private val companionObjectDescriptor = c.storageManager.createNullableLazyValue { computeCompanionObjectDescriptor() }
    private val sealedSubclasses = c.storageManager.createLazyValue { computeSubclassesForSealedClass() }
    private val inlineClassRepresentation = c.storageManager.createNullableLazyValue { computeInlineClassRepresentation() }

    internal val thisAsProtoContainer: ProtoContainer.Class = ProtoContainer.Class(
        classProto, c.nameResolver, c.typeTable, sourceElement,
        (containingDeclaration as? DeserializedClassDescriptor)?.thisAsProtoContainer
    )

    val versionRequirements: List<VersionRequirement>
        get() = VersionRequirement.create(classProto, c.nameResolver, c.versionRequirementTable)

    override val annotations =
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
            Annotations.EMPTY
        } else NonEmptyDeserializedAnnotations(c.storageManager) {
            c.components.annotationAndConstantLoader.loadClassAnnotations(thisAsProtoContainer).toList()
        }

    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getKind() = kind

    override fun getModality() = modality

    override fun getVisibility() = visibility

    override fun isInner() = Flags.IS_INNER.get(classProto.flags)

    override fun isData() = Flags.IS_DATA.get(classProto.flags)

    override fun isInline() = Flags.IS_INLINE_CLASS.get(classProto.flags) && metadataVersion.isAtMost(1, 4, 1)

    override fun isExpect() = Flags.IS_EXPECT_CLASS.get(classProto.flags)

    override fun isActual() = false

    override fun isExternal() = Flags.IS_EXTERNAL_CLASS.get(classProto.flags)

    override fun isFun() = Flags.IS_FUN_INTERFACE.get(classProto.flags)

    override fun isValue() = Flags.IS_INLINE_CLASS.get(classProto.flags) && metadataVersion.isAtLeast(1, 4, 2)

    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner): MemberScope =
        memberScopeHolder.getScope(kotlinTypeRefiner)

    override fun getStaticScope() = staticScope

    override fun isCompanionObject(): Boolean = Flags.CLASS_KIND.get(classProto.flags) == ProtoBuf.Class.Kind.COMPANION_OBJECT

    private fun computePrimaryConstructor(): ClassConstructorDescriptor? {
        if (kind.isSingleton) {
            return DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply {
                returnType = getDefaultType()
            }
        }

        return classProto.constructorList.firstOrNull { !Flags.IS_SECONDARY.get(it.flags) }?.let { constructorProto ->
            c.memberDeserializer.loadConstructor(constructorProto, true)
        }
    }

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = primaryConstructor()

    private fun computeConstructors(): Collection<ClassConstructorDescriptor> =
        computeSecondaryConstructors() + listOfNotNull(unsubstitutedPrimaryConstructor) +
                c.components.additionalClassPartsProvider.getConstructors(this)

    private fun computeSecondaryConstructors(): List<ClassConstructorDescriptor> =
        classProto.constructorList.filter { Flags.IS_SECONDARY.get(it.flags) }.map {
            c.memberDeserializer.loadConstructor(it, false)
        }

    override fun getConstructors() = constructors()

    override fun getContextReceivers(): List<ReceiverParameterDescriptor> = classProto.contextReceiverTypeList.map {
        val contextReceiverType = c.typeDeserializer.type(it)
        ReceiverParameterDescriptorImpl(
            thisAsReceiverParameter,
            ContextClassReceiver(this, contextReceiverType, null),
            Annotations.EMPTY
        );
    }

    private fun computeCompanionObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasCompanionObjectName()) return null

        val companionObjectName = c.nameResolver.getName(classProto.companionObjectName)
        return memberScope.getContributedClassifier(companionObjectName, NoLookupLocation.FROM_DESERIALIZATION) as? ClassDescriptor
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = companionObjectDescriptor()

    internal fun hasNestedClass(name: Name): Boolean =
        name in memberScope.classNames

    private fun computeSubclassesForSealedClass(): Collection<ClassDescriptor> {
        if (modality != Modality.SEALED) return emptyList()

        val fqNames = classProto.sealedSubclassFqNameList
        if (fqNames.isNotEmpty()) {
            return fqNames.mapNotNull { index ->
                c.components.deserializeClass(c.nameResolver.getClassId(index))
            }
        }

        // This is needed because classes compiled with Kotlin 1.0 did not contain the sealed_subclass_fq_name field
        return CliSealedClassInheritorsProvider.computeSealedSubclasses(this, allowSealedInheritorsInDifferentFilesOfSamePackage = false)
    }

    override fun getSealedSubclasses() = sealedSubclasses()

    override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? = inlineClassRepresentation()

    private fun computeInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? {
        if (!isInlineClass()) return null

        val propertyName = when {
            classProto.hasInlineClassUnderlyingPropertyName() ->
                c.nameResolver.getName(classProto.inlineClassUnderlyingPropertyName)
            !metadataVersion.isAtLeast(1, 5, 1) -> {
                // Before 1.5, inline classes did not have underlying property name & type in the metadata.
                // However, they were experimental, so supposedly this logic can be removed at some point in the future.
                val constructor = unsubstitutedPrimaryConstructor ?: error("Inline class has no primary constructor: $this")
                constructor.valueParameters.first().name
            }
            else -> error("Inline class has no underlying property name in metadata: $this")
        }

        val type = classProto.inlineClassUnderlyingType(c.typeTable)?.let(c.typeDeserializer::simpleType)
            ?: run {
                val underlyingProperty =
                    memberScope.getContributedVariables(propertyName, NoLookupLocation.FROM_DESERIALIZATION)
                        .singleOrNull { it.extensionReceiverParameter == null }
                        ?: error("Inline class has no underlying property: $this")
                underlyingProperty.type as SimpleType
            }

        return InlineClassRepresentation(propertyName, type)
    }

    override fun toString() =
        "deserialized ${if (isExpect) "expect " else ""}class $name" // not using descriptor renderer to preserve laziness

    override fun getSource() = sourceElement

    override fun getDeclaredTypeParameters() = c.typeDeserializer.ownTypeParameters

    override fun getDefaultFunctionTypeForSamInterface(): SimpleType? {
        return c.components.samConversionResolver.resolveFunctionTypeIfSamInterface(this)
    }

    override fun isDefinitelyNotSamInterface() = !isFun

    private inner class DeserializedClassTypeConstructor : AbstractClassTypeConstructor(c.storageManager) {
        private val parameters = c.storageManager.createLazyValue {
            this@DeserializedClassDescriptor.computeConstructorTypeParameters()
        }

        override fun computeSupertypes(): Collection<KotlinType> {
            val result = classProto.supertypes(c.typeTable).map { supertypeProto ->
                c.typeDeserializer.type(supertypeProto)
            } + c.components.additionalClassPartsProvider.getSupertypes(this@DeserializedClassDescriptor)

            val unresolved = result.mapNotNull { supertype ->
                supertype.constructor.declarationDescriptor as? NotFoundClasses.MockClassDescriptor
            }

            if (unresolved.isNotEmpty()) {
                c.components.errorReporter.reportIncompleteHierarchy(
                    this@DeserializedClassDescriptor,
                    unresolved.map { it.classId?.asSingleFqName()?.asString() ?: it.name.asString() }
                )
            }

            return result.toList()
        }

        override fun getParameters() = parameters()

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@DeserializedClassDescriptor

        override fun toString() = name.toString()

        override val supertypeLoopChecker: SupertypeLoopChecker
            // TODO: inject implementation
            get() = SupertypeLoopChecker.EMPTY
    }

    private inner class DeserializedClassMemberScope(private val kotlinTypeRefiner: KotlinTypeRefiner) : DeserializedMemberScope(
        c, classProto.functionList, classProto.propertyList, classProto.typeAliasList,
        classProto.nestedClassNameList.map(c.nameResolver::getName).let { { it } } // workaround KT-13454
    ) {
        private val classDescriptor: DeserializedClassDescriptor get() = this@DeserializedClassDescriptor

        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
        }

        private val refinedSupertypes = c.storageManager.createLazyValue {
            @OptIn(TypeRefinement::class)
            kotlinTypeRefiner.refineSupertypes(classDescriptor)
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = allDescriptors()

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
            recordLookup(name, location)
            return super.getContributedFunctions(name, location)
        }

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            recordLookup(name, location)
            return super.getContributedVariables(name, location)
        }

        override fun isDeclaredFunctionAvailable(function: SimpleFunctionDescriptor): Boolean {
            return c.components.platformDependentDeclarationFilter.isFunctionAvailable(this@DeserializedClassDescriptor, function)
        }

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableList<SimpleFunctionDescriptor>) {
            val fromSupertypes = ArrayList<SimpleFunctionDescriptor>()
            for (supertype in refinedSupertypes()) {
                fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED))
            }

            functions.addAll(c.components.additionalClassPartsProvider.getFunctions(name, this@DeserializedClassDescriptor))
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredProperties(name: Name, descriptors: MutableList<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in refinedSupertypes()) {
                fromSupertypes.addAll(supertype.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED))
            }
            generateFakeOverrides(name, fromSupertypes, descriptors)
        }

        private fun <D : CallableMemberDescriptor> generateFakeOverrides(
            name: Name,
            fromSupertypes: Collection<D>,
            result: MutableList<D>
        ) {
            val fromCurrent = ArrayList<CallableMemberDescriptor>(result)
            c.components.kotlinTypeChecker.overridingUtil.generateOverridesInFunctionGroup(
                name,
                fromSupertypes,
                fromCurrent,
                classDescriptor,
                object : NonReportingOverrideStrategy() {
                    override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                        // TODO: report "cannot infer visibility"
                        OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                        @Suppress("UNCHECKED_CAST")
                        result.add(fakeOverride as D)
                    }

                    override fun conflict(
                        fromSuper: CallableMemberDescriptor,
                        fromCurrent: CallableMemberDescriptor
                    ) {
                        // TODO report conflicts
                    }
                })
        }

        override fun getNonDeclaredFunctionNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.getFunctionNames()
            }.apply { addAll(c.components.additionalClassPartsProvider.getFunctionsNames(this@DeserializedClassDescriptor)) }
        }

        override fun getNonDeclaredVariableNames(): Set<Name> {
            return classDescriptor.typeConstructor.supertypes.flatMapTo(LinkedHashSet()) {
                it.memberScope.getVariableNames()
            }
        }

        override fun getNonDeclaredClassifierNames(): Set<Name>? {
            return classDescriptor.typeConstructor.supertypes.flatMapToNullable(LinkedHashSet()) {
                it.memberScope.getClassifierNames()
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            recordLookup(name, location)
            classDescriptor.enumEntries?.findEnumEntry(name)?.let { return it }
            return super.getContributedClassifier(name, location)
        }

        override fun createClassId(name: Name) = classId.createNestedClassId(name)

        override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.enumEntries?.all().orEmpty())
        }

        override fun recordLookup(name: Name, location: LookupLocation) {
            c.components.lookupTracker.record(location, classDescriptor, name)
        }
    }

    private inner class EnumEntryClassDescriptors {
        private val enumEntryProtos = classProto.enumEntryList.associateBy { c.nameResolver.getName(it.name) }

        private val enumEntryByName = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> { name ->

            enumEntryProtos[name]?.let { proto ->
                EnumEntrySyntheticClassDescriptor.create(
                    c.storageManager, this@DeserializedClassDescriptor, name, enumMemberNames,
                    DeserializedAnnotations(c.storageManager) {
                        c.components.annotationAndConstantLoader.loadEnumEntryAnnotations(thisAsProtoContainer, proto).toList()
                    },
                    SourceElement.NO_SOURCE
                )
            }
        }

        private val enumMemberNames = c.storageManager.createLazyValue { computeEnumMemberNames() }

        fun findEnumEntry(name: Name): ClassDescriptor? = enumEntryByName(name)

        private fun computeEnumMemberNames(): Set<Name> {
            // NOTE: order of enum entry members should be irrelevant
            // because enum entries are effectively invisible to user (as classes)
            val result = HashSet<Name>()

            for (supertype in getTypeConstructor().supertypes) {
                for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                    if (descriptor is SimpleFunctionDescriptor || descriptor is PropertyDescriptor) {
                        result.add(descriptor.name)
                    }
                }
            }

            return classProto.functionList.mapTo(result) { c.nameResolver.getName(it.name) } +
                    classProto.propertyList.mapTo(result) { c.nameResolver.getName(it.name) }
        }

        fun all(): Collection<ClassDescriptor> =
            enumEntryProtos.keys.mapNotNull { name -> findEnumEntry(name) }
    }
}
