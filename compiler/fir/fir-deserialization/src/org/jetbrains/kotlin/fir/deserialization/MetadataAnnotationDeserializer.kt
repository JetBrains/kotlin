/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.AnnotationDeserializer.CallableKind
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class MetadataAnnotationDeserializer(private val session: FirSession) :
    AnnotationDeserializer {
    override fun loadClassAnnotations(
        classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
    ): List<FirAnnotation> =
        /* Note that HAS_ANNOTATIONS flag has incorrect value for inline classes in the old syntax (`inline class ...`).
        For inline classes in the old syntax, JVM backend adds a `@JvmInline` annotation, but HAS_ANNOTATIONS flag is still false.
        So, we disable the optimization that avoids loading annotations, for inline classes. */
        loadAnnotationsFromMetadata(classProto.flags.takeUnless(Flags.IS_VALUE_CLASS::get), classProto.annotationList, nameResolver)

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(functionProto.flags, functionProto.annotationList, nameResolver)


    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(propertyProto.flags, propertyProto.annotationList, nameResolver, AnnotationUseSiteTarget.PROPERTY)

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(
            propertyProto.flags,
            propertyProto.backingFieldAnnotationList,
            nameResolver,
            AnnotationUseSiteTarget.FIELD
        )

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(
            propertyProto.flags,
            propertyProto.delegateFieldAnnotationList,
            nameResolver,
            AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD,
        )

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(getterFlags, propertyProto.getterAnnotationList, nameResolver)


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
        loadAnnotationsFromMetadata(valueParameterProto.flags, valueParameterProto.annotationList, nameResolver)


    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(flags = null, enumEntryProto.annotationList, nameResolver)

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: CallableKind,
    ): List<FirAnnotation> =
        when (callableProto) {
            is ProtoBuf.Function ->
                loadAnnotationsFromMetadata(flags = null, callableProto.extensionReceiverAnnotationList, nameResolver)
            is ProtoBuf.Property ->
                loadAnnotationsFromMetadata(flags = null, callableProto.extensionReceiverAnnotationList, nameResolver)
            else -> emptyList()
        }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(setterFlags, propertyProto.setterAnnotationList, nameResolver)


    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> =
        loadAnnotationsFromMetadata(constructorProto.flags, constructorProto.annotationList, nameResolver)


    private fun loadAnnotationsFromMetadata(
        flags: Int?, annotations: List<ProtoBuf.Annotation>, nameResolver: NameResolver, useSiteTarget: AnnotationUseSiteTarget? = null,
    ): List<FirAnnotation> =
        if (flags != null && !Flags.HAS_ANNOTATIONS.get(flags)) emptyList()
        else annotations.map { deserializeAnnotation(it, nameResolver, session, useSiteTarget) }
}
