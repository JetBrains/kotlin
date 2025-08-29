/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.deserialization

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
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class FirBuiltinAnnotationDeserializer(
    private val session: FirSession
) : AnnotationDeserializer {

    override fun inheritAnnotationInfo(parent: AnnotationDeserializer) = Unit

    override fun loadClassAnnotations(
        classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        return classProto.loadAnnotations(session, BuiltInSerializerProtocol.classAnnotation, classProto.flags, nameResolver)
    }

    override fun loadTypeAliasAnnotations(
        aliasProto: ProtoBuf.TypeAlias,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(aliasProto.flags)) return emptyList()
        return aliasProto.annotationList.map { deserializeAnnotation(session, it, nameResolver) }
    }

    override fun loadFunctionAnnotations(
        containerSource: DeserializedContainerSource?,
        functionProto: ProtoBuf.Function,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        return functionProto.loadAnnotations(session, BuiltInSerializerProtocol.functionAnnotation, functionProto.flags, nameResolver)
    }

    override fun loadPropertyAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        containingClassProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadPropertyBackingFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadPropertyDelegatedFieldAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadPropertyGetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        getterFlags: Int,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadPropertySetterAnnotations(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        setterFlags: Int,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadConstructorAnnotations(
        containerSource: DeserializedContainerSource?,
        constructorProto: ProtoBuf.Constructor,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadValueParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        valueParameterProto: ProtoBuf.ValueParameter,
        classProto: ProtoBuf.Class?,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: AnnotationDeserializer.CallableKind,
        parameterIndex: Int,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadEnumEntryAnnotations(
        classId: ClassId,
        enumEntryProto: ProtoBuf.EnumEntry,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadExtensionReceiverParameterAnnotations(
        containerSource: DeserializedContainerSource?,
        callableProto: MessageLite,
        nameResolver: NameResolver,
        typeTable: TypeTable,
        kind: AnnotationDeserializer.CallableKind,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadAnnotationPropertyDefaultValue(
        containerSource: DeserializedContainerSource?,
        propertyProto: ProtoBuf.Property,
        expectedPropertyType: FirTypeRef,
        nameResolver: NameResolver,
        typeTable: TypeTable,
    ): FirExpression? {
        TODO("Not yet implemented")
    }

    override fun loadTypeParameterAnnotations(
        typeParameterProto: ProtoBuf.TypeParameter,
        nameResolver: NameResolver,
    ): List<FirAnnotation> {
        TODO("Not yet implemented")
    }

    override fun loadTypeAnnotations(typeProto: ProtoBuf.Type, nameResolver: NameResolver): List<FirAnnotation> {
        if (!Flags.HAS_ANNOTATIONS.get(typeProto.flags)) return emptyList()
        val annotations = typeProto.getExtension(protocol.typeAnnotation).orEmpty()
        return annotations.map { deserializeAnnotation(session, it, nameResolver) }
    }
}


