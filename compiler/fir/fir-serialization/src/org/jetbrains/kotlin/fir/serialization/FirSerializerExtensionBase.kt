/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol

abstract class FirKotlinSerializerExtensionBase(private val protocol: SerializerExtensionProtocol) : FirSerializerExtension() {

    override fun serializeClass(
        klass: FirClass<*>,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
        for (annotation in klass.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.classAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeConstructor(
        constructor: FirConstructor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: FirElementSerializer
    ) {
        for (annotation in constructor.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.constructorAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeFunction(
        function: FirFunction<*>,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        for (annotation in function.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.functionAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
        for (annotation in property.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.propertyAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in property.getter?.nonSourceAnnotations(session).orEmpty()) {
            proto.addExtension(protocol.propertyGetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        for (annotation in property.setter?.nonSourceAnnotations(session).orEmpty()) {
            proto.addExtension(protocol.propertySetterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
        // TODO: compile-time initializer
    }

    override fun serializeEnumEntry(enumEntry: FirEnumEntry, proto: ProtoBuf.EnumEntry.Builder) {
        for (annotation in enumEntry.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.enumEntryAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeValueParameter(parameter: FirValueParameter, proto: ProtoBuf.ValueParameter.Builder) {
        for (annotation in parameter.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.parameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeType(type: ConeKotlinType, proto: ProtoBuf.Type.Builder) {
        // TODO: annotated type?
//        for (annotation in type.nonSourceAnnotations(session)) {
//            proto.addExtension(protocol.typeAnnotation, annotationSerializer.serializeAnnotation(annotation))
//        }
    }

    override fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
        for (annotation in typeParameter.nonSourceAnnotations(session)) {
            proto.addExtension(protocol.typeParameterAnnotation, annotationSerializer.serializeAnnotation(annotation))
        }
    }

    override fun serializeTypeAlias(typeAlias: FirTypeAlias, proto: ProtoBuf.TypeAlias.Builder) {
        // TODO serialize annotations on type aliases?
        // (this requires more extensive protobuf scheme modifications)
    }
}
