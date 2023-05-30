/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProvider
import org.jetbrains.kotlin.fir.serialization.constant.ConstValueProviderInternals
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.serialization.MutableVersionRequirementTable
import org.jetbrains.kotlin.name.FqName

abstract class FirSerializerExtension {
    abstract val session: FirSession

    abstract val stringTable: FirElementAwareStringTable

    abstract val metadataVersion: BinaryVersion

    val annotationSerializer by lazy { FirAnnotationSerializer(session, stringTable, constValueProvider) }

    protected abstract val constValueProvider: ConstValueProvider?
    protected abstract val additionalAnnotationsProvider: FirAdditionalMetadataAnnotationsProvider?

    @OptIn(ConstValueProviderInternals::class)
    internal inline fun <T> processFile(firFile: FirFile, crossinline action: () -> T): T {
        val previousFile = constValueProvider?.processingFirFile
        constValueProvider?.processingFirFile = firFile
        return try {
            action()
        } finally {
            constValueProvider?.processingFirFile = previousFile
        }
    }

    open fun shouldUseTypeTable(): Boolean = false
    open fun shouldUseNormalizedVisibility(): Boolean = false

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

    open fun serializeTypeAnnotations(annotations: List<FirAnnotation>, proto: ProtoBuf.Type.Builder) {
    }

    open fun serializeTypeParameter(typeParameter: FirTypeParameter, proto: ProtoBuf.TypeParameter.Builder) {
    }

    open fun serializeTypeAlias(typeAlias: FirTypeAlias, proto: ProtoBuf.TypeAlias.Builder) {
        for (annotation in typeAlias.nonSourceAnnotations(session)) {
            proto.addAnnotation(annotationSerializer.serializeAnnotation(annotation))
        }
    }

    fun hasAdditionalAnnotations(declaration: FirDeclaration): Boolean {
        return additionalAnnotationsProvider?.hasGeneratedAnnotationsFor(declaration) ?: false
    }

    // TODO: add usages
    fun getAnnotationsGeneratedByPlugins(declaration: FirDeclaration): List<FirAnnotation> {
        return additionalAnnotationsProvider?.findGeneratedAnnotationsFor(declaration) ?: emptyList()
    }

    open fun serializeErrorType(type: ConeErrorType, builder: ProtoBuf.Type.Builder) {
        error("Cannot serialize error type: $type")
    }
}
