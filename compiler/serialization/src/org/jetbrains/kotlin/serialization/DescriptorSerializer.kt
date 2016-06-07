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

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.isEnumEntry
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.Interner
import org.jetbrains.kotlin.utils.rethrow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class DescriptorSerializer private constructor(
        private val containingDeclaration: DeclarationDescriptor?,
        private val typeParameters: Interner<TypeParameterDescriptor>,
        private val extension: SerializerExtension,
        private val typeTable: MutableTypeTable,
        private val serializeTypeTableToFunction: Boolean) {

    fun serialize(message: MessageLite): ByteArray {
        try {
            val result = ByteArrayOutputStream()
            stringTable.serializeTo(result)
            message.writeTo(result)
            return result.toByteArray()
        }
        catch (e: IOException) {
            throw rethrow(e)
        }

    }

    private fun createChildSerializer(callable: CallableDescriptor): DescriptorSerializer {
        return DescriptorSerializer(callable, Interner(typeParameters), extension, typeTable, false)
    }

    val stringTable: StringTable
        get() = extension.stringTable

    private fun useTypeTable(): Boolean {
        return extension.shouldUseTypeTable()
    }

    fun classProto(classDescriptor: ClassDescriptor): ProtoBuf.Class.Builder {
        val builder = ProtoBuf.Class.newBuilder()

        val flags = Flags.getClassFlags(hasAnnotations(classDescriptor), classDescriptor.visibility, classDescriptor.modality,
                                        classDescriptor.kind, classDescriptor.isInner, classDescriptor.isCompanionObject,
                                        classDescriptor.isData)
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

        for (descriptor in sort(DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope))) {
            if (descriptor is CallableMemberDescriptor) {
                if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue

                if (descriptor is PropertyDescriptor) {
                    builder.addProperty(propertyProto(descriptor))
                }
                else if (descriptor is FunctionDescriptor) {
                    builder.addFunction(functionProto(descriptor))
                }
            }
        }

        for (descriptor in sort(DescriptorUtils.getAllDescriptors(classDescriptor.unsubstitutedInnerClassesScope))) {
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

        val companionObjectDescriptor = classDescriptor.companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            builder.companionObjectName = getSimpleNameIndex(companionObjectDescriptor.name)
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        extension.serializeClass(classDescriptor, builder)

        return builder
    }

    fun propertyProto(descriptor: PropertyDescriptor): ProtoBuf.Property.Builder {
        val builder = ProtoBuf.Property.newBuilder()

        val local = createChildSerializer(descriptor)

        var hasGetter = false
        var hasSetter = false
        val lateInit = descriptor.isLateInit
        val isConst = descriptor.isConst

        val compileTimeConstant = descriptor.compileTimeInitializer
        val hasConstant = !(compileTimeConstant == null || compileTimeConstant is NullValue)

        val hasAnnotations = !descriptor.annotations.getAllAnnotations().isEmpty()

        val propertyFlags = Flags.getAccessorFlags(
                hasAnnotations,
                descriptor.visibility,
                descriptor.modality,
                false,
                false)

        val getter = descriptor.getGetter()
        if (getter != null) {
            hasGetter = true
            val accessorFlags = getAccessorFlags(getter!!)
            if (accessorFlags != propertyFlags) {
                builder.getterFlags = accessorFlags
            }
        }

        val setter = descriptor.getSetter()
        if (setter != null) {
            hasSetter = true
            val accessorFlags = getAccessorFlags(setter!!)
            if (accessorFlags != propertyFlags) {
                builder.setterFlags = accessorFlags
            }

            if (!setter!!.isDefault()) {
                val setterLocal = local.createChildSerializer(setter!!)
                for (valueParameterDescriptor in setter!!.getValueParameters()) {
                    builder.setSetterValueParameter(setterLocal.valueParameter(valueParameterDescriptor))
                }
            }
        }

        val flags = Flags.getPropertyFlags(
                hasAnnotations, descriptor.visibility, descriptor.modality, descriptor.kind, descriptor.isVar,
                hasGetter, hasSetter, hasConstant, isConst, lateInit)
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

        extension.serializeProperty(descriptor, builder)

        return builder
    }

    fun functionProto(descriptor: FunctionDescriptor): ProtoBuf.Function.Builder {
        val builder = ProtoBuf.Function.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getFunctionFlags(
                hasAnnotations(descriptor), descriptor.visibility, descriptor.modality, descriptor.kind,
                descriptor.isOperator, descriptor.isInfix, descriptor.isInline, descriptor.isTailrec,
                descriptor.isExternal, descriptor.isSuspend
        )
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        if (useTypeTable()) {
            //noinspection ConstantConditions
            builder.returnTypeId = local.typeId(descriptor.returnType!!)
        }
        else {
            //noinspection ConstantConditions
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

        extension.serializeFunction(descriptor, builder)

        return builder
    }

    fun constructorProto(descriptor: ConstructorDescriptor): ProtoBuf.Constructor.Builder {
        val builder = ProtoBuf.Constructor.newBuilder()

        val local = createChildSerializer(descriptor)

        val flags = Flags.getConstructorFlags(hasAnnotations(descriptor), descriptor.visibility, !descriptor.isPrimary)
        if (flags != builder.flags) {
            builder.flags = flags
        }

        for (valueParameterDescriptor in descriptor.valueParameters) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor))
        }

        extension.serializeConstructor(descriptor, builder)

        return builder
    }

    fun typeAliasProto(descriptor: TypeAliasDescriptor): ProtoBuf.TypeAlias.Builder {
        val builder = ProtoBuf.TypeAlias.newBuilder()

        val flags = Flags.getTypeAliasFlags(hasAnnotations(descriptor), descriptor.visibility)
        if (flags != builder.flags) {
            builder.flags = flags
        }

        builder.name = getSimpleNameIndex(descriptor.name)

        for (typeParameterDescriptor in descriptor.declaredTypeParameters) {
            builder.addTypeParameter(typeParameter(typeParameterDescriptor))
        }

        val underlyingType = descriptor.underlyingType
        if (useTypeTable()) {
            builder.underlyingTypeId = typeId(underlyingType)
        }
        else {
            builder.setUnderlyingType(type(underlyingType))
        }

        val expandedType = descriptor.expandedType
        if (useTypeTable()) {
            builder.expandedTypeId = typeId(expandedType)
        }
        else {
            builder.setExpandedType(type(expandedType))
        }

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

        val flags = Flags.getValueParameterFlags(hasAnnotations(descriptor), descriptor.declaresDefaultValue(),
                                                 descriptor.isCrossinline, descriptor.isNoinline, descriptor.isCoroutine)
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

    private fun typeId(type: KotlinType): Int {
        return typeTable[type(type)]
    }

    private fun type(type: KotlinType): ProtoBuf.Type.Builder {
        val builder = ProtoBuf.Type.newBuilder()

        if (type.isError) {
            extension.serializeErrorType(type, builder)
            return builder
        }

        if (type.isFlexible()) {
            val flexibleType = type.asFlexibleType()

            val lowerBound = type(flexibleType.lowerBound)
            val upperBound = type(flexibleType.upperBound)
            extension.serializeFlexibleType(flexibleType, lowerBound, upperBound);
            if (useTypeTable()) {
                lowerBound.flexibleUpperBoundId = typeTable[upperBound]
            }
            else {
                lowerBound.setFlexibleUpperBound(upperBound)
            }
            return lowerBound
        }

        val descriptor = type.constructor.declarationDescriptor
        if (descriptor is ClassDescriptor) {
            val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType should not be null in case of class")

            fillFromPossiblyInnerType(builder, possiblyInnerType)
        }
        else if (descriptor is TypeParameterDescriptor) {
            if (descriptor.containingDeclaration === containingDeclaration) {
                builder.typeParameterName = getSimpleNameIndex(descriptor.name)
            }
            else {
                builder.typeParameter = getTypeParameterId(descriptor)
            }

            assert(type.arguments.isEmpty()) { "Found arguments for type constructor build on type parameter: " + descriptor }
        }
        else if (descriptor is TypeAliasDescriptor) {
            val possiblyInnerType = type.buildPossiblyInnerType() ?: error("possiblyInnerType should not be null in case of type alias")
            fillFromPossiblyInnerType(builder, possiblyInnerType)
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

    private fun fillFromPossiblyInnerType(
            builder: ProtoBuf.Type.Builder,
            type: PossiblyInnerType) {
        val classifierDescriptor = type.classifierDescriptor
        val classifierId = getClassifierId(classifierDescriptor)
        if (classifierDescriptor is ClassDescriptor) {
            builder.className = classifierId
        }
        else if (classifierDescriptor is TypeAliasDescriptor) {
            builder.typeAliasName = classifierId
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

    @JvmOverloads fun packageProto(
            fragments: Collection<PackageFragmentDescriptor>,
            skip: Function1<DeclarationDescriptor, Boolean>? = null): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        val members = ArrayList<DeclarationDescriptor>()
        for (fragment in fragments) {
            members.addAll(DescriptorUtils.getAllDescriptors(fragment.getMemberScope()))
        }

        for (declaration in sort(members)) {
            if (skip != null && skip.invoke(declaration)) continue

            if (declaration is PropertyDescriptor) {
                builder.addProperty(propertyProto(declaration))
            }
            else if (declaration is FunctionDescriptor) {
                builder.addFunction(functionProto(declaration))
            }
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        extension.serializePackage(builder)

        return builder
    }

    fun packagePartProto(members: Collection<DeclarationDescriptor>): ProtoBuf.Package.Builder {
        val builder = ProtoBuf.Package.newBuilder()

        for (declaration in sort(members)) {
            if (declaration is PropertyDescriptor) {
                builder.addProperty(propertyProto(declaration))
            }
            else if (declaration is FunctionDescriptor) {
                builder.addFunction(functionProto(declaration))
            }
            else if (declaration is TypeAliasDescriptor) {
                builder.addTypeAlias(typeAliasProto(declaration))
            }
        }

        val typeTableProto = typeTable.serialize()
        if (typeTableProto != null) {
            builder.typeTable = typeTableProto
        }

        extension.serializePackage(builder)

        return builder
    }

    private fun getClassifierId(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        return stringTable.getFqNameIndex(descriptor)
    }

    private fun getSimpleNameIndex(name: Name): Int {
        return stringTable.getStringIndex(name.asString())
    }

    private fun getTypeParameterId(descriptor: TypeParameterDescriptor): Int {
        return typeParameters.intern(descriptor)
    }

    companion object {
        @JvmStatic
        fun createTopLevel(extension: SerializerExtension): DescriptorSerializer {
            return DescriptorSerializer(null, Interner<TypeParameterDescriptor>(), extension, MutableTypeTable(), false)
        }

        @JvmStatic
        fun createForLambda(extension: SerializerExtension): DescriptorSerializer {
            return DescriptorSerializer(null, Interner<TypeParameterDescriptor>(), extension, MutableTypeTable(), true)
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
                    false)
            for (typeParameter in descriptor.declaredTypeParameters) {
                serializer.typeParameters.intern(typeParameter)
            }
            return serializer
        }

        private fun getAccessorFlags(accessor: PropertyAccessorDescriptor): Int {
            return Flags.getAccessorFlags(
                    hasAnnotations(accessor),
                    accessor.visibility,
                    accessor.modality,
                    !accessor.isDefault,
                    accessor.isExternal)
        }

        private fun variance(variance: Variance): ProtoBuf.TypeParameter.Variance {
            when (variance) {
                Variance.INVARIANT -> return ProtoBuf.TypeParameter.Variance.INV
                Variance.IN_VARIANCE -> return ProtoBuf.TypeParameter.Variance.IN
                Variance.OUT_VARIANCE -> return ProtoBuf.TypeParameter.Variance.OUT
            }
            throw IllegalStateException("Unknown variance: " + variance)
        }

        private fun projection(projectionKind: Variance): ProtoBuf.Type.Argument.Projection {
            when (projectionKind) {
                Variance.INVARIANT -> return ProtoBuf.Type.Argument.Projection.INV
                Variance.IN_VARIANCE -> return ProtoBuf.Type.Argument.Projection.IN
                Variance.OUT_VARIANCE -> return ProtoBuf.Type.Argument.Projection.OUT
            }
            throw IllegalStateException("Unknown projectionKind: " + projectionKind)
        }

        private fun hasAnnotations(descriptor: Annotated): Boolean {
            return !descriptor.annotations.isEmpty()
        }

        fun <T : DeclarationDescriptor> sort(descriptors: Collection<T>): List<T> {
            val result = ArrayList(descriptors)
            //NOTE: the exact comparator does matter here
            Collections.sort(result, MemberComparator.INSTANCE)
            return result

        }
    }
}
