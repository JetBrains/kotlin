/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.transformSuspendFunctionToRuntimeFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.RequireKotlinNames
import org.jetbrains.kotlin.resolve.checkers.KotlinVersionStringAnnotationValueChecker
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.serialization.deserialization.descriptors.VersionRequirement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.Interner
import java.io.ByteArrayOutputStream
import java.util.*

class DescriptorSerializer private constructor(
        private val containingDeclaration: DeclarationDescriptor?,
        private val typeParameters: Interner<TypeParameterDescriptor>,
        private val extension: SerializerExtension,
        private val typeTable: MutableTypeTable,
        private val versionRequirementTable: MutableVersionRequirementTable,
        private val serializeTypeTableToFunction: Boolean
) {
    private val contractSerializer = ContractSerializer()

    fun serialize(message: MessageLite): ByteArray {
        return ByteArrayOutputStream().apply {
            stringTable.serializeTo(this)
            message.writeTo(this)
        }.toByteArray()
    }

    private fun createChildSerializer(descriptor: DeclarationDescriptor): DescriptorSerializer =
            DescriptorSerializer(descriptor, Interner(typeParameters), extension, typeTable, versionRequirementTable,
                                 serializeTypeTableToFunction = false)

    val stringTable: StringTable
        get() = extension.stringTable

    private fun useTypeTable(): Boolean = extension.shouldUseTypeTable()

    fun classProto(classDescriptor: ClassDescriptor): ProtoBuf.Class.Builder {
        val builder = ProtoBuf.Class.newBuilder()

        val flags = Flags.getClassFlags(
                hasAnnotations(classDescriptor), normalizeVisibility(classDescriptor), classDescriptor.modality, classDescriptor.kind,
                classDescriptor.isInner, classDescriptor.isCompanionObject, classDescriptor.isData, classDescriptor.isExternal,
                classDescriptor.isExpect
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.fqName = getClassifierId(classDescriptor)

        for (typeParameterDescriptor in classDescriptor.declaredTypeParameters) {
            builder.addTypeParameter(typeParameter(typeParameterDescriptor))
        }

        if (!KotlinBuiltIns.isSpecialClassWithNoSupertypes(classDescriptor)) {
            // Special classes (Any, Nothing) have no supertypes
            for (supertype in classDescriptor.typeConstructor.supertypes) {
                if (useTypeTable()) {
                    builder.addSupertypeId(typeId(supertype))
                }
                else {
                    builder.addSupertype(type(supertype))
                }
            }
        }

        for (descriptor in classDescriptor.constructors) {
            builder.addConstructor(constructorProto(descriptor))
        }

        val callableMembers =
                extension.customClassMembersProducer?.getCallableMembers(classDescriptor)
                ?: sort(
                        DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)
                                .filterIsInstance<CallableMemberDescriptor>()
                )

        for (descriptor in callableMembers) {
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue

            when (descriptor) {
                is PropertyDescriptor -> builder.addProperty(propertyProto(descriptor))
                is FunctionDescriptor -> builder.addFunction(functionProto(descriptor))
            }
        }

        val nestedClassifiers = sort(DescriptorUtils.getAllDescriptors(classDescriptor.unsubstitutedInnerClassesScope))
        for (descriptor in nestedClassifiers) {
            if (descriptor is TypeAliasDescriptor) {
                builder.addTypeAlias(typeAliasProto(descriptor))
            }
            else {
                val name = getSimpleNameIndex(descriptor.name)
                if (isEnumEntry(descriptor)) {
                    builder.addEnumEntry(enumEntryProto(descriptor as ClassDescriptor))
                }
                else {
                    builder.addNestedClassName(name)
                }
            }
        }

        for (sealedSubclass in classDescriptor.sealedSubclasses) {
            builder.addSealedSubclassFqName(getClassifierId(sealedSubclass))
        }

        val companionObjectDescriptor = classDescriptor.companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            builder.companionObjectName = getSimpleNameIndex(companionObjectDescriptor.name)
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        val requirement = serializeVersionRequirement(classDescriptor)
        if (requirement != null) {
            builder.versionRequirement = requirement
        }

        val versionRequirementTableProto = versionRequirementTable.serialize()
        if (versionRequirementTableProto != null) {
            builder.versionRequirementTable = versionRequirementTableProto
        }

        extension.serializeClass(classDescriptor, builder)
        return builder
    }

    fun propertyProto(descriptor: PropertyDescriptor): ProtoBuf.Property.Builder {
        val builder = ProtoBuf.Property.newBuilder()

        val local = createChildSerializer(descriptor)

        var hasGetter = false
        var hasSetter = false

        val compileTimeConstant = descriptor.compileTimeInitializer
        val hasConstant = compileTimeConstant != null && compileTimeConstant !is NullValue

        val hasAnnotations = descriptor.annotations.getAllAnnotations().isNotEmpty()

        val propertyFlags = Flags.getAccessorFlags(hasAnnotations, normalizeVisibility(descriptor), descriptor.modality, false, false, false)

        val getter = descriptor.getter
        if (getter != null) {
            hasGetter = true
            val accessorFlags = getAccessorFlags(getter)
            if (accessorFlags != propertyFlags) {
                builder.getterFlags = accessorFlags
            }
        }

        val setter = descriptor.setter
        if (setter != null) {
            hasSetter = true
            val accessorFlags = getAccessorFlags(setter)
            if (accessorFlags != propertyFlags) {
                builder.setterFlags = accessorFlags
            }

            if (!setter.isDefault) {
                val setterLocal = local.createChildSerializer(setter)
                for (valueParameterDescriptor in setter.valueParameters) {
                    builder.setSetterValueParameter(setterLocal.valueParameter(valueParameterDescriptor))
                }
            }
        }

        val flags = Flags.getPropertyFlags(
                hasAnnotations, normalizeVisibility(descriptor), descriptor.modality, descriptor.kind, descriptor.isVar,
                hasGetter, hasSetter, hasConstant, descriptor.isConst, descriptor.isLateInit, descriptor.isExternal,
                @Suppress("DEPRECATION") descriptor.isDelegated, descriptor.isExpect
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(descriptor.type)
        }
        else {
            builder.setReturnType(local.type(descriptor.type))
        }

        for (typeParameterDescriptor in descriptor.typeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverParameter.type)
            }
            else {
                builder.setReceiverType(local.type(receiverParameter.type))
            }
        }

        val requirement = serializeVersionRequirement(descriptor)
        if (requirement != null) {
            builder.versionRequirement = requirement
        }
        else if (descriptor.isSuspendOrHasSuspendTypesInSignature()) {
            builder.versionRequirement = writeVersionRequirement(LanguageFeature.Coroutines)
        }

        extension.serializeProperty(descriptor, builder)

        return builder
    }

    private fun normalizeVisibility(descriptor: DeclarationDescriptorWithVisibility) =
            // It can be necessary for Java classes serialization having package-private visibility
            if (extension.shouldUseNormalizedVisibility())
                descriptor.visibility.normalize()
            else
                descriptor.visibility

    fun functionProto(descriptor: FunctionDescriptor): ProtoBuf.Function.Builder {
        val builder = ProtoBuf.Function.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getFunctionFlags(
                hasAnnotations(descriptor), normalizeVisibility(descriptor), descriptor.modality, descriptor.kind, descriptor.isOperator,
                descriptor.isInfix, descriptor.isInline, descriptor.isTailrec, descriptor.isExternal, descriptor.isSuspend,
                descriptor.isExpect
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.returnTypeId = local.typeId(descriptor.returnType!!)
        }
        else {
            builder.setReturnType(local.type(descriptor.returnType!!))
        }

        for (typeParameterDescriptor in descriptor.typeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val receiverParameter = descriptor.extensionReceiverParameter
        if (receiverParameter != null) {
            if (useTypeTable()) {
                builder.receiverTypeId = local.typeId(receiverParameter.type)
            }
            else {
                builder.setReceiverType(local.type(receiverParameter.type))
            }
        }

        for (valueParameterDescriptor in descriptor.valueParameters) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor))
        }

        if (serializeTypeTableToFunction) {
            val typeTableProto = typeTable.serialize()
            if (typeTableProto != null) {
                builder.typeTable = typeTableProto
            }
        }

        val requirement = serializeVersionRequirement(descriptor)
        if (requirement != null) {
            builder.versionRequirement = requirement
        }
        else if (descriptor.isSuspendOrHasSuspendTypesInSignature()) {
            builder.versionRequirement = writeVersionRequirement(LanguageFeature.Coroutines)
        }

        contractSerializer.serializeContractOfFunctionIfAny(descriptor, builder, this)

        extension.serializeFunction(descriptor, builder)

        return builder
    }

    fun constructorProto(descriptor: ConstructorDescriptor): ProtoBuf.Constructor.Builder {
        val builder = ProtoBuf.Constructor.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getConstructorFlags(hasAnnotations(descriptor), normalizeVisibility(descriptor), !descriptor.isPrimary)
        if (flags != builder.flags) {
            builder.flags = flags
        }

        for (valueParameterDescriptor in descriptor.valueParameters) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor))
        }

        val requirement = serializeVersionRequirement(descriptor)
        if (requirement != null) {
            builder.versionRequirement = requirement
        }
        else if (descriptor.isSuspendOrHasSuspendTypesInSignature()) {
            builder.versionRequirement = writeVersionRequirement(LanguageFeature.Coroutines)
        }

        extension.serializeConstructor(descriptor, builder)

        return builder
    }

    private fun CallableMemberDescriptor.isSuspendOrHasSuspendTypesInSignature(): Boolean {
        if (this is FunctionDescriptor && isSuspend) return true

        return listOfNotNull(
                extensionReceiverParameter?.type,
                returnType,
                *valueParameters.map(ValueParameterDescriptor::getType).toTypedArray()
        ).any { type -> type.contains(UnwrappedType::isSuspendFunctionType) }
    }

    fun typeAliasProto(descriptor: TypeAliasDescriptor): ProtoBuf.TypeAlias.Builder {
        val builder = ProtoBuf.TypeAlias.newBuilder()
        val local = createChildSerializer(descriptor)

        val flags = Flags.getTypeAliasFlags(hasAnnotations(descriptor), normalizeVisibility(descriptor))
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        for (typeParameterDescriptor in descriptor.declaredTypeParameters) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor))
        }

        val underlyingType = descriptor.underlyingType
        if (useTypeTable()) {
            builder.underlyingTypeId = local.typeId(underlyingType)
        }
        else {
            builder.setUnderlyingType(local.type(underlyingType))
        }

        val expandedType = descriptor.expandedType
        if (useTypeTable()) {
            builder.expandedTypeId = local.typeId(expandedType)
        }
        else {
            builder.setExpandedType(local.type(expandedType))
        }

        val requirement = serializeVersionRequirement(descriptor)
        if (requirement != null) {
            builder.versionRequirement = requirement
        }

        builder.addAllAnnotation(descriptor.annotations.map { extension.annotationSerializer.serializeAnnotation(it) })

        return builder
    }

    fun enumEntryProto(descriptor: ClassDescriptor): ProtoBuf.EnumEntry.Builder {
        val builder = ProtoBuf.EnumEntry.newBuilder()
        builder.name = getSimpleNameIndex(descriptor.name)
        extension.serializeEnumEntry(descriptor, builder)
        return builder
    }

    private fun valueParameter(descriptor: ValueParameterDescriptor): ProtoBuf.ValueParameter.Builder {
        val builder = ProtoBuf.ValueParameter.newBuilder()

        val flags = Flags.getValueParameterFlags(
                hasAnnotations(descriptor), descriptor.declaresDefaultValue(),
                descriptor.isCrossinline, descriptor.isNoinline
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            builder.typeId = typeId(descriptor.type)
        }
        else {
            builder.setType(type(descriptor.type))
        }

        val varargElementType = descriptor.varargElementType
        if (varargElementType != null) {
            if (useTypeTable()) {
                builder.varargElementTypeId = typeId(varargElementType)
            }
            else {
                builder.setVarargElementType(type(varargElementType))
            }
        }

        extension.serializeValueParameter(descriptor, builder)

        return builder
    }

    private fun typeParameter(typeParameter: TypeParameterDescriptor): ProtoBuf.TypeParameter.Builder {
        val builder = ProtoBuf.TypeParameter.newBuilder()

        builder.id = getTypeParameterId(typeParameter)

        builder.name = getSimpleNameIndex(typeParameter.name)

        if (typeParameter.isReified != builder.reified) {
            builder.reified = typeParameter.isReified
        }

        val variance = variance(typeParameter.variance)
        if (variance != builder.variance) {
            builder.variance = variance
        }
        extension.serializeTypeParameter(typeParameter, builder)

        val upperBounds = typeParameter.upperBounds
        if (upperBounds.size == 1 && KotlinBuiltIns.isDefaultBound(upperBounds.single())) return builder

        for (upperBound in upperBounds) {
            if (useTypeTable()) {
                builder.addUpperBoundId(typeId(upperBound))
            }
            else {
                builder.addUpperBound(type(upperBound))
            }
        }

        return builder
    }

    internal fun typeId(type: KotlinType): Int = typeTable[type(type)]

    internal fun type(type: KotlinType): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()

        if (type.isError) {
            extension.serializeErrorType(type, builder)
            return builder
        }

        if (type.isFlexible()) {
            val flexibleType = type.asFlexibleType()

            val lowerBound = type(flexibleType.lowerBound)
            val upperBound = type(flexibleType.upperBound)
            extension.serializeFlexibleType(flexibleType, lowerBound, upperBound)
            if (useTypeTable()) {
                lowerBound.flexibleUpperBoundId = typeTable[upperBound]
            }
            else {
                lowerBound.setFlexibleUpperBound(upperBound)
            }
            return lowerBound
        }

        if (type.isSuspendFunctionType) {
            val functionType = type(transformSuspendFunctionToRuntimeFunctionType(type))
            functionType.flags = Flags.getTypeFlags(true)
            return functionType
        }

        val descriptor = type.constructor.declarationDescriptor
        when (descriptor) {
            is ClassDescriptor, is TypeAliasDescriptor -> {
                val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType should not be null: $type")
                fillFromPossiblyInnerType(builder, possiblyInnerType)
            }
            is TypeParameterDescriptor -> {
                if (descriptor.containingDeclaration === containingDeclaration) {
                    builder.typeParameterName = getSimpleNameIndex(descriptor.name)
                }
                else {
                    builder.typeParameter = getTypeParameterId(descriptor)
                }

                assert(type.arguments.isEmpty()) { "Found arguments for type constructor build on type parameter: $descriptor" }
            }
        }

        if (type.isMarkedNullable != builder.nullable) {
            builder.nullable = type.isMarkedNullable
        }

        val abbreviation = type.getAbbreviatedType()?.abbreviation
        if (abbreviation != null) {
            if (useTypeTable()) {
                builder.abbreviatedTypeId = typeId(abbreviation)
            }
            else {
                builder.setAbbreviatedType(type(abbreviation))
            }
        }

        extension.serializeType(type, builder)

        return builder
    }

    private fun fillFromPossiblyInnerType(builder: ProtoBuf.Type.Builder, type: PossiblyInnerType) {
        val classifierDescriptor = type.classifierDescriptor
        val classifierId = getClassifierId(classifierDescriptor)
        when (classifierDescriptor) {
            is ClassDescriptor -> builder.className = classifierId
            is TypeAliasDescriptor -> builder.typeAliasName = classifierId
        }

        for (projection in type.arguments) {
            builder.addArgument(typeArgument(projection))
        }

        if (type.outerType != null) {
            val outerBuilder = ProtoBuf.Type.newBuilder()
            fillFromPossiblyInnerType(outerBuilder, type.outerType!!)
            if (useTypeTable()) {
                builder.outerTypeId = typeTable[outerBuilder]
            }
            else {
                builder.setOuterType(outerBuilder)
            }
        }
    }

    private fun typeArgument(typeProjection: TypeProjection): ProtoBuf.Type.Argument.Builder {
        val builder = ProtoBuf.Type.Argument.newBuilder()

        if (typeProjection.isStarProjection) {
            builder.projection = ProtoBuf.Type.Argument.Projection.STAR
        }
        else {
            val projection = projection(typeProjection.projectionKind)

            if (projection != builder.projection) {
                builder.projection = projection
            }

            if (useTypeTable()) {
                builder.typeId = typeId(typeProjection.type)
            }
            else {
                builder.setType(type(typeProjection.type))
            }
        }

        return builder
    }

    fun packagePartProto(packageFqName: FqName, members: Collection<DeclarationDescriptor>): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        for (declaration in sort(members)) {
            when (declaration) {
                is PropertyDescriptor -> builder.addProperty(propertyProto(declaration))
                is FunctionDescriptor -> builder.addFunction(functionProto(declaration))
                is TypeAliasDescriptor -> builder.addTypeAlias(typeAliasProto(declaration))
            }
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        val versionRequirementTableProto = versionRequirementTable.serialize()
        if (versionRequirementTableProto != null) {
            builder.versionRequirementTable = versionRequirementTableProto
        }

        extension.serializePackage(packageFqName, builder)

        return builder
    }

    private fun writeVersionRequirement(languageFeature: LanguageFeature): Int {
        val languageVersion = languageFeature.sinceVersion!!
        val requirement = ProtoBuf.VersionRequirement.newBuilder().apply {
            VersionRequirement.Version(languageVersion.major, languageVersion.minor).encode(
                    writeVersion = { version = it },
                    writeVersionFull = { versionFull = it }
            )
        }
        return versionRequirementTable[requirement]
    }

    // Returns index into versionRequirementTable, or null if there's no @RequireKotlin on the descriptor
    private fun serializeVersionRequirement(descriptor: DeclarationDescriptor): Int? {
        val annotation = descriptor.annotations.findAnnotation(RequireKotlinNames.FQ_NAME) ?: return null
        val args = annotation.allValueArguments

        val versionString = (args[RequireKotlinNames.VERSION] as? StringValue)?.value ?: return null
        val matchResult = KotlinVersionStringAnnotationValueChecker.VERSION_REGEX.matchEntire(versionString) ?: return null

        val major = matchResult.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = matchResult.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        val patch = matchResult.groupValues.getOrNull(4)?.toIntOrNull() ?: 0

        val proto = ProtoBuf.VersionRequirement.newBuilder()
        VersionRequirement.Version(major, minor, patch).encode(
                writeVersion = { proto.version = it },
                writeVersionFull = { proto.versionFull = it }
        )

        val message = (args[RequireKotlinNames.MESSAGE] as? StringValue)?.value
        if (message != null) {
            proto.message = stringTable.getStringIndex(message)
        }

        val level = (args[RequireKotlinNames.LEVEL] as? EnumValue)?.enumEntryName?.asString()
        when (level) {
            DeprecationLevel.ERROR.name -> { /* ERROR is the default level */ }
            DeprecationLevel.WARNING.name -> proto.level = ProtoBuf.VersionRequirement.Level.WARNING
            DeprecationLevel.HIDDEN.name -> proto.level = ProtoBuf.VersionRequirement.Level.HIDDEN
        }

        val versionKind = (args[RequireKotlinNames.VERSION_KIND] as? EnumValue)?.enumEntryName?.asString()
        when (versionKind) {
            ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION.name -> { /* LANGUAGE_VERSION is the default kind */ }
            ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.COMPILER_VERSION
            ProtoBuf.VersionRequirement.VersionKind.API_VERSION.name ->
                proto.versionKind = ProtoBuf.VersionRequirement.VersionKind.API_VERSION
        }

        val errorCode = (args[RequireKotlinNames.ERROR_CODE] as? IntValue)?.value
        if (errorCode != null && errorCode != -1) {
            proto.errorCode = errorCode
        }

        return versionRequirementTable[proto]
    }

    private fun getClassifierId(descriptor: ClassifierDescriptorWithTypeParameters): Int =
            stringTable.getFqNameIndex(descriptor)

    private fun getSimpleNameIndex(name: Name): Int =
            stringTable.getStringIndex(name.asString())

    private fun getTypeParameterId(descriptor: TypeParameterDescriptor): Int =
            typeParameters.intern(descriptor)

    private fun getAccessorFlags(accessor: PropertyAccessorDescriptor): Int {
        return Flags.getAccessorFlags(
                hasAnnotations(accessor),
                normalizeVisibility(accessor),
                accessor.modality,
                !accessor.isDefault,
                accessor.isExternal,
                accessor.isInline
        )
    }

    companion object {
        @JvmStatic
        fun createTopLevel(extension: SerializerExtension): DescriptorSerializer {
            return DescriptorSerializer(null, Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                                        serializeTypeTableToFunction = false)
        }

        @JvmStatic
        fun createForLambda(extension: SerializerExtension): DescriptorSerializer {
            return DescriptorSerializer(null, Interner(), extension, MutableTypeTable(), MutableVersionRequirementTable(),
                                        serializeTypeTableToFunction = true)
        }

        @JvmStatic
        fun create(descriptor: ClassDescriptor, extension: SerializerExtension): DescriptorSerializer {
            val container = descriptor.containingDeclaration
            val parentSerializer = if (container is ClassDescriptor)
                create(container, extension)
            else
                createTopLevel(extension)

            // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
            // serializing outer classes before nested classes.
            // Otherwise our interner can get wrong ids because we may serialize classes in any order.
            val serializer = DescriptorSerializer(
                    descriptor,
                    Interner(parentSerializer.typeParameters),
                    parentSerializer.extension,
                    MutableTypeTable(),
                    MutableVersionRequirementTable(),
                    serializeTypeTableToFunction = false
            )
            for (typeParameter in descriptor.declaredTypeParameters) {
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }

        private fun variance(variance: Variance): ProtoBuf.TypeParameter.Variance = when (variance) {
            Variance.INVARIANT -> ProtoBuf.TypeParameter.Variance.INV
            Variance.IN_VARIANCE -> ProtoBuf.TypeParameter.Variance.IN
            Variance.OUT_VARIANCE -> ProtoBuf.TypeParameter.Variance.OUT
        }

        private fun projection(projectionKind: Variance): ProtoBuf.Type.Argument.Projection = when (projectionKind) {
            Variance.INVARIANT -> ProtoBuf.Type.Argument.Projection.INV
            Variance.IN_VARIANCE -> ProtoBuf.Type.Argument.Projection.IN
            Variance.OUT_VARIANCE -> ProtoBuf.Type.Argument.Projection.OUT
        }

        private fun hasAnnotations(descriptor: Annotated): Boolean = !descriptor.annotations.isEmpty()

        fun <T : DeclarationDescriptor> sort(descriptors: Collection<T>): List<T> =
                ArrayList(descriptors).apply {
                    //NOTE: the exact comparator does matter here
                    Collections.sort(this, MemberComparator.INSTANCE)
                }
    }
}
