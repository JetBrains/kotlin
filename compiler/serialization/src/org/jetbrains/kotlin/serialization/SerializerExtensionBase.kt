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

package org.jetbrains.kotlin.serialization

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.descriptorUtil.nonSourceAnnotations
import org.jetbrains.kotlin.types.KotlinType

abstract class KotlinSerializerExtensionBase(private val protocol: SerializerExtensionProtocol) : SerializerExtension() {
    override val stringTable = StringTableImpl()

    override fun serializeClass(
        descriptor: ClassDescriptor,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.classAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
        proto.setExtension(protocol.packageFqName, stringTable.getPackageFqNameIndex(packageFqName))
    }

    override fun serializeConstructor(
        descriptor: ConstructorDescriptor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.constructorAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeFunction(
        descriptor: FunctionDescriptor,
        proto: ProtoBuf.Function.Builder,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.functionAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeProperty(
        descriptor: PropertyDescriptor,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: DescriptorSerializer
    ) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.propertyAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in descriptor.getter?.nonSourceAnnotations.orEmpty()) {
            proto.addExtension(protocol.propertyGetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in descriptor.setter?.nonSourceAnnotations.orEmpty()) {
            proto.addExtension(protocol.propertySetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        val constantInitializer = descriptor.compileTimeInitializer ?: return
        if (constantInitializer !is NullValue) {
            proto.setExtension(protocol.compileTimeValue, annotationSerializer.valueProto(constantInitializer).build())
        }
    }

    override fun serializeEnumEntry(descriptor: ClassDescriptor, proto: ProtoBuf.EnumEntry.Builder) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.enumEntryAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeValueParameter(descriptor: ValueParameterDescriptor, proto: ProtoBuf.ValueParameter.Builder) {
        for (annotation in descriptor.nonSourceAnnotations) {
            proto.addExtension(protocol.parameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeType(type: KotlinType, proto: ProtoBuf.Type.Builder) {
        for (annotation in type.nonSourceAnnotations) {
            proto.addExtension(protocol.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeParameter(typeParameter: TypeParameterDescriptor, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations) {
            proto.addExtension(protocol.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeAlias(typeAlias: TypeAliasDescriptor, proto: ProtoBuf.TypeAlias.Builder) {
        // TODO serialize annotations on type aliases?
        // (this requires more extensive protobuf scheme modifications)
    }

}
