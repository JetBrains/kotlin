/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.SerializerExtensionProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

abstract class AbstractAnnotationDeserializer(
    private val session: FirSession,
    protected val protocol: SerializerExtensionProtocol
) {
    open fun inheritAnnotationInfo(parent: AbstractAnnotationDeserializer) {
    }

    enum class CallableKind {
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        OTHERS
    }

    open fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(classProto.flags)) return emptyList()
        val annotations = classProto.getExtension(protocol.classAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(session, it, nameResolver) }
    }

    fun loadTypeAliasAnnotations(aliasProto: ProtoBuf.TypeAlias, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(aliasProto.flags)) return emptyList()
        return aliasProto.annotationList.map { deserializeAnnotation(session, it, nameResolver) }
    }

    open fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(functionProto.flags)) return emptyList()
        val annotations = functionProto.getExtension(protocol.functionAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(session, it, nameResolver) }
    }

    open fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            session, protocol.propertyAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY
        )
    }

    open fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            session, protocol.propertyBackingFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.FIELD
        )
    }

    open fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            session, protocol.propertyDelegatedFieldAnnotation, propertyProto.flags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
        )
    }

    open fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            session, protocol.propertyGetterAnnotation, getterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_GETTER
        )
    }

    open fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int
    ): List<FirAnnotation> {
        return propertyProto.loadAnnotations(
            session, protocol.propertySetterAnnotation, setterFlags, nameResolver,
            AnnotationUseSiteTarget.PROPERTY_SETTER
        )
    }

    open fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): List<FirAnnotation> {
        return constructorProto.loadAnnotations(session, protocol.constructorAnnotation, constructorProto.flags, nameResolver)
    }

    open fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
        parameterIndex: Int
    ): List<FirAnnotation> {
        return valueParameterProto.loadAnnotations(session, protocol.parameterAnnotation, valueParameterProto.flags, nameResolver)
    }

    open fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        return enumEntryProto.loadAnnotations(session, protocol.enumEntryAnnotation, -1, nameResolver)
    }

    open fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> {
        return when (callableProto) {
            is ProtoBuf.Property -> callableProto.loadAnnotations(
                session, protocol.propertyExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            is ProtoBuf.Function -> callableProto.loadAnnotations(
                session, protocol.functionExtensionReceiverAnnotation, callableProto.flags, nameResolver,
            )
            else -> emptyList()
        }
    }

    open fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable
    ): FirExpression? {
        return null
    }

    abstract fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation>

    open fun loadTypeParameterAnnotations(typeParameterProto: ProtoBuf.TypeParameter, nameResolver: NameResolver): List<FirAnnotation> =
        emptyList<FirAnnotation>()
}
