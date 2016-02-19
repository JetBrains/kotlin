/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AbstractClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverridingStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.upperIfFlexible
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import org.jetbrains.kotlin.utils.toReadOnlyList
import java.util.*

class DeserializedClassDescriptor(
        outerContext: DeserializationContext,
        val classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        private val sourceElement: SourceElement
) : ClassDescriptor, AbstractClassDescriptor(
        outerContext.storageManager,
        nameResolver.getClassId(classProto.fqName).shortClassName
) {
    private val modality = Deserialization.modality(Flags.MODALITY.get(classProto.flags))
    private val visibility = Deserialization.visibility(Flags.VISIBILITY.get(classProto.flags))
    private val kindFromProto = Flags.CLASS_KIND.get(classProto.flags)
    private val kind = Deserialization.classKind(kindFromProto)
    private val isCompanion = kindFromProto == ProtoBuf.Class.Kind.COMPANION_OBJECT
    private val isInner = Flags.IS_INNER.get(classProto.flags)
    private val isData = Flags.IS_DATA.get(classProto.flags)

    val c = outerContext.childContext(this, classProto.typeParameterList, nameResolver, TypeTable(classProto.typeTable))

    private val classId = nameResolver.getClassId(classProto.fqName)

    private val staticScope = StaticScopeForKotlinClass(this)
    private val typeConstructor = DeserializedClassTypeConstructor()
    private val memberScope = DeserializedClassMemberScope()
    private val nestedClasses = NestedClassDescriptors()
    private val enumEntries = if (kind == ClassKind.ENUM_CLASS) EnumEntryClassDescriptors() else null

    private val containingDeclaration = outerContext.containingDeclaration
    private val primaryConstructor = c.storageManager.createNullableLazyValue { computePrimaryConstructor() }
    private val constructors = c.storageManager.createLazyValue { computeConstructors() }
    private val companionObjectDescriptor = c.storageManager.createNullableLazyValue { computeCompanionObjectDescriptor() }

    private val annotations =
            if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
                Annotations.EMPTY
            }
            else DeserializedAnnotations(c.storageManager) {
                c.components.annotationAndConstantLoader.loadClassAnnotations(classProto, c.nameResolver)
            }

    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration

    override fun getTypeConstructor(): TypeConstructor = typeConstructor

    override fun getKind() = kind

    override fun getModality() = modality

    override fun getVisibility() = visibility

    override fun isInner() = isInner

    override fun isData() = isData

    override fun getAnnotations() = annotations

    override fun getUnsubstitutedMemberScope(): MemberScope = memberScope

    override fun getStaticScope() = staticScope

    override fun isCompanionObject(): Boolean = isCompanion

    private fun computePrimaryConstructor(): ConstructorDescriptor? {
        if (kind.isSingleton) {
            return DescriptorFactory.createPrimaryConstructorForObject(this, SourceElement.NO_SOURCE).apply {
                returnType = getDefaultType()
            }
        }

        return classProto.constructorList.firstOrNull { !Flags.IS_SECONDARY.get(it.flags) }?.let { constructorProto ->
            c.memberDeserializer.loadConstructor(constructorProto, true)
        }
    }

    override fun getUnsubstitutedPrimaryConstructor(): ConstructorDescriptor? = primaryConstructor()

    private fun computeConstructors(): Collection<ConstructorDescriptor> =
            computeSecondaryConstructors() + getUnsubstitutedPrimaryConstructor().singletonOrEmptyList()

    private fun computeSecondaryConstructors(): List<ConstructorDescriptor> =
            classProto.constructorList.filter { Flags.IS_SECONDARY.get(it.flags) }.map {
                c.memberDeserializer.loadConstructor(it, false)
            }

    override fun getConstructors() = constructors()

    private fun computeCompanionObjectDescriptor(): ClassDescriptor? {
        if (!classProto.hasCompanionObjectName()) return null

        val companionObjectName = c.nameResolver.getName(classProto.companionObjectName)
        return memberScope.getContributedClassifier(companionObjectName, NoLookupLocation.FROM_DESERIALIZATION) as? ClassDescriptor
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = companionObjectDescriptor()

    private fun computeSupertypes(): Collection<KotlinType> {
        val result = ArrayList<KotlinType>(classProto.supertypeCount)
        val unresolved = ArrayList<DeserializedType>(0)

        for (supertypeProto in classProto.supertypes(c.typeTable)) {
            val supertype = c.typeDeserializer.type(supertypeProto)
            if (supertype.isError) {
                unresolved.add(supertype.upperIfFlexible() as? DeserializedType ?: error("Not a deserialized type: $supertype"))
            }
            else {
                result.add(supertype)
            }
        }

        result.addAll(c.components.additionalSupertypes.forClass(this))

        if (unresolved.isNotEmpty()) {
            c.components.errorReporter.reportIncompleteHierarchy(this, unresolved.map(DeserializedType::getPresentableText))
        }

        return result.toReadOnlyList()
    }

    internal fun hasNestedClass(name: Name): Boolean {
        return name in nestedClasses.nestedClassNames
    }

    override fun toString() = "deserialized class ${getName().toString()}" // not using descriptor render to preserve laziness

    override fun getSource() = sourceElement

    override fun getDeclaredTypeParameters() = c.typeDeserializer.ownTypeParameters

    private inner class DeserializedClassTypeConstructor : AbstractClassTypeConstructor() {
        private val supertypes = c.storageManager.createLazyValue {
            computeSupertypes()
        }

        private val parameters = c.storageManager.createLazyValue {
            this@DeserializedClassDescriptor.computeConstructorTypeParameters()
        }

        override fun getParameters() = parameters()

        override fun getSupertypes() = supertypes()

        override fun isFinal(): Boolean = isFinalClass

        override fun isDenotable() = true

        override fun getDeclarationDescriptor() = this@DeserializedClassDescriptor

        override fun getAnnotations(): Annotations = Annotations.EMPTY // TODO

        override fun toString() = getName().toString()
    }

    private inner class DeserializedClassMemberScope : DeserializedMemberScope(c, classProto.functionList, classProto.propertyList) {
        private val classDescriptor: DeserializedClassDescriptor get() = this@DeserializedClassDescriptor
        private val allDescriptors = c.storageManager.createLazyValue {
            computeDescriptors(DescriptorKindFilter.ALL, MemberScope.ALL_NAME_FILTER, NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS)
        }

        override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                               nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> = allDescriptors()

        override fun computeNonDeclaredFunctions(name: Name, functions: MutableCollection<FunctionDescriptor>) {
            val fromSupertypes = ArrayList<FunctionDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                fromSupertypes.addAll(supertype.memberScope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED))
            }
            generateFakeOverrides(name, fromSupertypes, functions)
        }

        override fun computeNonDeclaredProperties(name: Name, descriptors: MutableCollection<PropertyDescriptor>) {
            val fromSupertypes = ArrayList<PropertyDescriptor>()
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                fromSupertypes.addAll(supertype.memberScope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED))
            }
            generateFakeOverrides(name, fromSupertypes, descriptors)
        }

        private fun <D : CallableMemberDescriptor> generateFakeOverrides(name: Name, fromSupertypes: Collection<D>, result: MutableCollection<D>) {
            val fromCurrent = ArrayList<CallableMemberDescriptor>(result)
            OverridingUtil.generateOverridesInFunctionGroup(name, fromSupertypes, fromCurrent, classDescriptor, object : NonReportingOverrideStrategy() {
                override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                    // TODO: report "cannot infer visibility"
                    OverridingUtil.resolveUnknownVisibilityForMember(fakeOverride, null)
                    @Suppress("UNCHECKED_CAST")
                    result.add(fakeOverride as D)
                }

                override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                    // TODO report conflicts
                }
            })
        }

        override fun addNonDeclaredDescriptors(result: MutableCollection<DeclarationDescriptor>, location: LookupLocation) {
            for (supertype in classDescriptor.getTypeConstructor().supertypes) {
                for (descriptor in supertype.memberScope.getContributedDescriptors()) {
                    if (descriptor is FunctionDescriptor) {
                        result.addAll(getContributedFunctions(descriptor.name, location))
                    }
                    else if (descriptor is PropertyDescriptor) {
                        result.addAll(getContributedVariables(descriptor.name, location))
                    }
                    // Nothing else is inherited
                }
            }
        }

        override fun getClassDescriptor(name: Name): ClassifierDescriptor? =
                classDescriptor.enumEntries?.findEnumEntry(name) ?: classDescriptor.nestedClasses.findNestedClass(name)

        override fun addClassDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.nestedClasses.all())
        }

        override fun addEnumEntryDescriptors(result: MutableCollection<DeclarationDescriptor>, nameFilter: (Name) -> Boolean) {
            result.addAll(classDescriptor.enumEntries?.all().orEmpty())
        }
    }

    private inner class NestedClassDescriptors {
        internal val nestedClassNames = nestedClassNames()

        internal val findNestedClass = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
            name ->
            if (name in nestedClassNames) {
                c.components.deserializeClass(classId.createNestedClassId(name))
            }
            else null
        }

        private fun nestedClassNames(): Set<Name> {
            val result = LinkedHashSet<Name>()
            val nameResolver = c.nameResolver
            for (index in classProto.nestedClassNameList) {
                result.add(nameResolver.getName(index!!))
            }
            return result
        }

        fun all(): Collection<ClassDescriptor> {
            val result = ArrayList<ClassDescriptor>(nestedClassNames.size)
            nestedClassNames.forEach { name -> result.addIfNotNull(findNestedClass(name)) }
            return result
        }
    }

    private inner class EnumEntryClassDescriptors {
        private val enumEntryProtos = classProto.enumEntryList.associateBy { c.nameResolver.getName(it.name) }
        private val protoContainer =
                ProtoContainer.Class(classProto, c.nameResolver, c.typeTable, (containingDeclaration as? ClassDescriptor)?.kind)

        val enumEntryByName = c.storageManager.createMemoizedFunctionWithNullableValues<Name, ClassDescriptor> {
            name ->

            enumEntryProtos[name]?.let { proto ->
                EnumEntrySyntheticClassDescriptor.create(
                        c.storageManager, this@DeserializedClassDescriptor, name, enumMemberNames,
                        DeserializedAnnotations(c.storageManager) {
                            c.components.annotationAndConstantLoader.loadEnumEntryAnnotations(protoContainer, proto)
                        },
                        SourceElement.NO_SOURCE
                )
            }
        }

        private val enumMemberNames = c.storageManager.createLazyValue { computeEnumMemberNames() }

        fun findEnumEntry(name: Name): ClassDescriptor? = enumEntryByName(name)

        private fun computeEnumMemberNames(): Collection<Name> {
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
