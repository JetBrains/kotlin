/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.ConeTypeProjectionPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.types.ConeCapturedType
import org.jetbrains.kotlin.fir.types.ConeCapturedTypeConstructor
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.renderForDebugging
import org.jetbrains.kotlin.types.model.CaptureStatus

internal class KaFirCapturedType(
    override val coneType: ConeCapturedType,
    private val builder: KaSymbolByFirBuilder,
) : KaCapturedType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token
    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val projection: KaTypeProjection
        get() = withValidityAssertion { builder.typeBuilder.buildTypeProjection(coneType.constructor.projection) }

    override val annotationsList: KaAnnotationsList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaAnalysisNonPublicApi
    override fun createPointer(): KaTypePointer<KaCapturedType> = withValidityAssertion {
        val typeParameterSymbolPointer = (coneType.constructor.typeParameterMarker as? ConeTypeParameterLookupTag)
            ?.typeParameterSymbol
            ?.let { builder.classifierBuilder.buildTypeParameterSymbol(it) }
            ?.createPointer()

        return KaFirCapturedTypePointer(
            captureStatus = coneType.captureStatus,
            lowerTypePointer = coneType.lowerType?.let { builder.typeBuilder.buildKtType(it).createPointer() },
            coneNullability = coneType.nullability,
            coneProjectionPointer = ConeTypeProjectionPointer(coneType.constructor.projection, builder),
            supertypePointers = coneType.constructor.supertypes?.map { builder.typeBuilder.buildKtType(it).createPointer() },
            typeParameterSymbolPointer = typeParameterSymbolPointer,
            isProjectionNotNull = coneType.isProjectionNotNull
        )
    }
}

@KaAnalysisNonPublicApi
private class KaFirCapturedTypePointer(
    private val captureStatus: CaptureStatus,
    private val lowerTypePointer: KaTypePointer<*>?,
    private val coneNullability: ConeNullability,
    private val coneProjectionPointer: ConeTypeProjectionPointer,
    private val supertypePointers: List<KaTypePointer<*>>?,
    private val typeParameterSymbolPointer: KaSymbolPointer<KaTypeParameterSymbol>?,
    private val isProjectionNotNull: Boolean
) : KaTypePointer<KaCapturedType> {
    override fun restore(session: KaSession): KaCapturedType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val lowerConeType = if (lowerTypePointer != null) {
            val lowerType = lowerTypePointer.restore(session) as? KaFirType ?: return null
            lowerType.coneType
        } else {
            null
        }

        val constructorProjection = coneProjectionPointer.restore(session) ?: return null

        val constructorSupertypes = if (supertypePointers != null) {
            buildList<ConeKotlinType>(supertypePointers.size) {
                for (supertypePointer in supertypePointers) {
                    val supertype = supertypePointer.restore(session) as? KaFirType ?: return null
                    add(supertype.coneType)
                }
            }
        } else {
            null
        }

        val constructorTypeParameterMarker = if (typeParameterSymbolPointer != null) {
            val typeParameterSymbol = with(session) { typeParameterSymbolPointer.restoreSymbol() } ?: return null
            typeParameterSymbol.firSymbol.toLookupTag()
        } else {
            null
        }

        val coneConstructor = ConeCapturedTypeConstructor(
            projection = constructorProjection,
            supertypes = constructorSupertypes,
            typeParameterMarker = constructorTypeParameterMarker
        )

        val coneType = ConeCapturedType(
            captureStatus = captureStatus,
            lowerType = lowerConeType,
            nullability = coneNullability,
            constructor = coneConstructor,
            isProjectionNotNull = isProjectionNotNull
        )

        return KaFirCapturedType(coneType, session.firSymbolBuilder)
    }
}