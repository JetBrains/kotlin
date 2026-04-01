/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AnnotationDeserializer.CallableKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class AnnotationDeserializerWithProtocol(
    private val session: FirSession,
    protected val protocol: SerializerExtensionProtocol
) : AnnotationDeserializer() {
    override fun inheritAnnotationInfo(parent: AnnotationDeserializer) {}

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> {
        return classProto.loadAnnotationsFromProtocol(session, protocol.classAnnotation, nameResolver)
    }

    override fun loadTypeAliasAnnotations(aliasProto: ProtoBuf.TypeAlias, nameResolver: NameResolver): List<FirAnnotation> {
        return loadAnnotationsFromMetadata(session, aliasProto.annotationList, nameResolver)
    }

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return functionProto.loadAnnotationsFromProtocol(session, protocol.functionAnnotation, nameResolver)
    }

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotationsFromProtocol(
            session, protocol.propertyAnnotation, nameResolver, AnnotationUseSiteTarget.PROPERTY
        )
    }

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotationsFromProtocol(
            session, protocol.propertyBackingFieldAnnotation, nameResolver, AnnotationUseSiteTarget.FIELD
        )
    }

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotationsFromProtocol(
            session, protocol.propertyDelegatedFieldAnnotation, nameResolver, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        )
    }

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotationsFromProtocol(
            session, protocol.propertyGetterAnnotation, nameResolver, AnnotationUseSiteTarget.PROPERTY_GETTER
        )
    }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotationsFromProtocol(
            session, protocol.propertySetterAnnotation, nameResolver, AnnotationUseSiteTarget.PROPERTY_SETTER
        )
    }

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return constructorProto.loadAnnotationsFromProtocol(session, protocol.constructorAnnotation, nameResolver)
    }

    override fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int
    ): List<FirAnnotation> {
        return valueParameterProto.loadAnnotationsFromProtocol(session, protocol.parameterAnnotation, nameResolver)
    }

    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        return enumEntryProto.loadAnnotationsFromProtocol(session, protocol.enumEntryAnnotation, nameResolver)
    }

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> {
        return when (callableProto) {
            is ProtoBuf.Property -> callableProto.loadAnnotationsFromProtocol(
                session, protocol.propertyExtensionReceiverAnnotation, nameResolver,
            )
            is ProtoBuf.Function -> callableProto.loadAnnotationsFromProtocol(
                session, protocol.functionExtensionReceiverAnnotation, nameResolver,
            )
            else -> emptyList()
        }
    }

    override fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): FirExpression? {
        return null
    }

    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> {
        return typeProto.loadAnnotationsFromProtocol(session, protocol.typeAnnotation, nameResolver)
    }

    override fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> {
        return typeParameterProto.loadAnnotationsFromProtocol(session, protocol.typeParameterAnnotation, nameResolver)
    }
}
