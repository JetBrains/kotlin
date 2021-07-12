/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName

abstract class FirSerializerExtension {
    abstract val session: FirSession

    abstract val stringTable: FirElementAwareStringTable

    abstract val metadataVersion: BinaryVersion

    val annotationSerializer by lazy { FirAnnotationSerializer(session, stringTable) }

    open fun shouldSerializeNestedClass(nestedClass: FirRegularClass): Boolean = true
    open fun shouldSerializeTypeAlias(typeAlias: FirTypeAlias): Boolean = true
    open fun shouldUseTypeTable(): Boolean = false
    open fun shouldUseNormalizedVisibility(): Boolean = false
    open fun shouldSerializeFunction(function: FirFunction): Boolean = false
    open fun shouldSerializeProperty(property: FirProperty): Boolean = false

    open fun serializePackage(packageFqName: FqName, proto: ProtoBuf.Package.Builder) {
    }

    open fun serializeClass(
        klass: FirClass,
        proto: ProtoBuf.Class.Builder,
        versionRequirementTable: MutableVersionRequirementTable,
        childSerializer: FirElementSerializer
    ) {
    }

    open fun serializeConstructor(
        constructor: FirConstructor,
        proto: ProtoBuf.Constructor.Builder,
        childSerializer: FirElementSerializer
    ) {
    }

    open fun serializeFunction(
        function: FirFunction,
        proto: ProtoBuf.Function.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
    }

    open fun serializeProperty(
        property: FirProperty,
        proto: ProtoBuf.Property.Builder,
        versionRequirementTable: MutableVersionRequirementTable?,
        childSerializer: FirElementSerializer
    ) {
    }

    open fun serializeEnumEntry(enumEntry: FirEnumEntry, proto: ProtoBuf.EnumEntry.Builder) {
    }

    open fun serializeValueParameter(parameter: FirValueParameter, proto: ProtoBuf.ValueParameter.Builder) {
    }

    open fun serializeFlexibleType(type: ConeFlexibleType, lowerProto: ProtoBuf.Type.Builder, upperProto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeAnnotation(annotation: FirAnnotationCall, proto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
    }

    open fun serializeTypeAlias(typeAlias: FirTypeAlias, proto: ProtoBuf.TypeAlias.Builder) {
    }

    open fun serializeErrorType(type: ConeKotlinErrorType, builder: ProtoBuf.Type.Builder) {
        throw IllegalStateException("Cannot serialize error type: $type")
    }

    open val customClassMembersProducer: ClassMembersProducer?
        get() = null

    interface ClassMembersProducer {
        fun getCallableMembers(klass: FirClass): Collection<FirCallableDeclaration>
    }

}
