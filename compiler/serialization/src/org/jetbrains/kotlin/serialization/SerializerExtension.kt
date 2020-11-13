/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType

abstract class SerializerExtension {
    abstract val stringTable: DescriptorAwareStringTable

    abstract val metadataVersion: BinaryVersion

    val annotationSerializer by lazy { createAnnotationSerializer() }

    protected open fun createAnnotationSerializer(): AnnotationSerializer = AnnotationSerializer(stringTable)

    open fun shouldUseTypeTable(): Boolean = false
    open fun shouldUseNormalizedVisibility(): Boolean = false
    open fun shouldSerializeFunction(descriptor: FunctionDescriptor): Boolean = true
    open fun shouldSerializeProperty(descriptor: PropertyDescriptor): Boolean = true
    open fun shouldSerializeTypeAlias(descriptor: TypeAliasDescriptor): Boolean = true
    open fun shouldSerializeNestedClass(descriptor: ClassDescriptor): Boolean = true

    interface ClassMembersProducer {
        fun getCallableMembers(classDescriptor: ClassDescriptor): Collection<CallableMemberDescriptor>
    }

    open val customClassMembersProducer: ClassMembersProducer?
        get() = null


    open fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
    }

    open fun serializeConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
    }

    open fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
    }

    open fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
    }

    open fun serializeFlexibleType(flexibleType: FlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
    }

    open fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
    }

    open fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
    }

    open fun serializeErrorType(type: KotlinType, builder: ProtoBuf.Type.Builder) {
        throw IllegalStateException("Cannot serialize error type: $type")
    }

    open fun releaseCoroutines(): Boolean = false

    open fun useOldInlineClassesManglingScheme(): Boolean = false
}
