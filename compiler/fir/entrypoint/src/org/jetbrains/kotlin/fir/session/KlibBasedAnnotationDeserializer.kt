/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AnnotationDeserializerWithProtocol
import org.jetbrains.kotlin.fir.deserialization.loadAnnotationsFromMetadataGuarded
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class KlibBasedAnnotationDeserializer(private val session: FirSession) :
    AnnotationDeserializerWithProtocol(session, KlibMetadataSerializerProtocol) {

    override fun loadClassAnnotations(classProto: ProtoBuf.Class, nameResolver: NameResolver): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            classProto.flags,
            classProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadClassAnnotations(classProto, nameResolver)

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            functionProto.flags,
            functionProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadFunctionAnnotations(containerSource, functionProto, nameResolver, typeTable)

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            propertyProto.flags,
            propertyProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
            AnnotationUseSiteTarget.PROPERTY,
        ) ?: super.loadPropertyAnnotations(containerSource, propertyProto, containingClassProto, nameResolver, typeTable)

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            constructorProto.flags,
            constructorProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadConstructorAnnotations(containerSource, constructorProto, nameResolver, typeTable)

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
        loadAnnotationsFromMetadataGuarded(
            session,
            valueParameterProto.flags,
            valueParameterProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadValueParameterAnnotations(
            containerSource,
            callableProto,
            valueParameterProto,
            classProto,
            nameResolver,
            typeTable,
            kind,
            parameterIndex
        )

    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            flags = null,
            enumEntryProto.annotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadEnumEntryAnnotations(classId, enumEntryProto, nameResolver)

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            getterFlags,
            propertyProto.getterAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadPropertyGetterAnnotations(containerSource, propertyProto, nameResolver, typeTable, getterFlags)

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            setterFlags,
            propertyProto.setterAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        ) ?: super.loadPropertySetterAnnotations(containerSource, propertyProto, nameResolver, typeTable, setterFlags)

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            propertyProto.flags,
            propertyProto.backingFieldAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
            AnnotationUseSiteTarget.FIELD,
        ) ?: super.loadPropertyBackingFieldAnnotations(containerSource, propertyProto, nameResolver, typeTable)

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadataGuarded(
            session,
            propertyProto.flags,
            propertyProto.delegateFieldAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD,
        ) ?: super.loadPropertyDelegatedFieldAnnotations(containerSource, propertyProto, nameResolver, typeTable)

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> = when (callableProto) {
        is ProtoBuf.Function -> loadAnnotationsFromMetadataGuarded(
            session,
            flags = null,
            callableProto.extensionReceiverAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        )
        is ProtoBuf.Property -> loadAnnotationsFromMetadataGuarded(
            session,
            flags = null,
            callableProto.extensionReceiverAnnotationList,
            nameResolver,
            LanguageFeature.KlibAnnotationsInCommonMetadata,
        )
        else -> null
    } ?: super.loadExtensionReceiverParameterAnnotations(containerSource, callableProto, nameResolver, typeTable, kind)
}
