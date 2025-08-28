/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.ExtendableMessage
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class AbstractAnnotationDeserializerWithProtocol(session: FirSession, protected val protocol: SerializerExtensionProtocol) :
    AbstractAnnotationDeserializer(session) {

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) return emptyList()
        val annotations = classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return emptyList()
        val annotations = functionProto.getExtension(protocol.functionAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(it, nameResolver) }
    }

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        propertyProto.loadAnnotations(
            protocol.propertyAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY
        )

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        propertyProto.loadAnnotations(
            protocol.propertyBackingFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.FIELD
        )

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        propertyProto.loadAnnotations(
            protocol.propertyDelegatedFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        )

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> =
        propertyProto.loadAnnotations(
            protocol.propertyGetterAnnotation, getterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_GETTER
        )

    override fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int,
    ): List<FirAnnotation> =
        valueParameterProto.loadAnnotations(protocol.parameterAnnotation, valueParameterProto.flags, nameResolver)

    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> =
        enumEntryProto.loadAnnotations(protocol.enumEntryAnnotation, -1, nameResolver)

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> =
        when (callableProto) {
            is ProtoBuf.Property -> callableProto.loadAnnotations(
                protocol.propertyExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            is ProtoBuf.Function -> callableProto.loadAnnotations(
                protocol.functionExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            else -> emptyList()
        }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int,
    ): List<FirAnnotation> =
        propertyProto.loadAnnotations(
            protocol.propertySetterAnnotation, setterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_SETTER
        )

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        constructorProto.loadAnnotations(protocol.constructorAnnotation, constructorProto.flags, nameResolver)

    private fun <T : ExtendableMessage<T>> T.loadAnnotations(
        extension: GeneratedMessageLite.GeneratedExtension<T, List<ProtoBuf.Annotation>>?,
        flags: Int,
        nameResolver: NameResolver,
        useSiteTarget: AnnotationUseSiteTarget? = null,
    ): List<FirAnnotation> {
        if (extension == null || flags >= 0 && !Flags.HAS_ANNOTATIONS.get(flags)) return emptyList()
        val annotations = getExtension(extension)
        return annotations.map { deserializeAnnotation(it, nameResolver, useSiteTarget) }
    }
}
