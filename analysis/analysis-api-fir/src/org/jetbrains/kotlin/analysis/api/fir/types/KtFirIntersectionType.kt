/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.types

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.annotations.KaFirAnnotationListForType
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.analysis.api.types.KaUsualClassType
import org.jetbrains.kotlin.analysis.utils.errors.requireIsInstance
import org.jetbrains.kotlin.fir.types.ConeIntersectionType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.renderForDebugging

internal class KaFirIntersectionType(
    override val coneType: ConeIntersectionType,
    private val builder: KaSymbolByFirBuilder,
) : KaIntersectionType(), KaFirType {
    override val token: KaLifetimeToken get() = builder.token

    override val conjuncts: List<KaType> by cached {
        coneType.intersectedTypes.map { conjunct -> builder.typeBuilder.buildKtType(conjunct) }
    }
    override val annotationsList: KaAnnotationsList by cached {
        KaFirAnnotationListForType.create(coneType, builder)
    }
    override val nullability: KaTypeNullability get() = withValidityAssertion { coneType.nullability.asKtNullability() }

    override val abbreviatedType: KaUsualClassType?
        get() = withValidityAssertion { null }

    override fun equals(other: Any?) = typeEquals(other)
    override fun hashCode() = typeHashcode()
    override fun toString() = coneType.renderForDebugging()

    @KaAnalysisNonPublicApi
    override fun createPointer(): KaTypePointer<KaIntersectionType> = withValidityAssertion {
        return KaFirIntersectionTypePointer(
            conjunctPointers = conjuncts.map { it.createPointer() },
            upperBoundForApproximationPointer = coneType.upperBoundForApproximation?.let(builder.typeBuilder::buildKtType)?.createPointer()
        )
    }
}

@KaAnalysisNonPublicApi
private class KaFirIntersectionTypePointer(
    private val conjunctPointers: List<KaTypePointer<*>>,
    private val upperBoundForApproximationPointer: KaTypePointer<*>?
) : KaTypePointer<KaIntersectionType> {
    override fun restore(session: KaSession): KaIntersectionType? = session.withValidityAssertion {
        requireIsInstance<KaFirSession>(session)

        val conjunctConeTypes = buildList(conjunctPointers.size) {
            for (conjunctPointer in conjunctPointers) {
                val conjunctType = conjunctPointer.restore(session) as? KaFirType ?: return null
                add(conjunctType.coneType)
            }
        }

        val upperBoundForApproximationConeType = if (upperBoundForApproximationPointer != null) {
            val upperBoundForApproximationType = upperBoundForApproximationPointer.restore(session) as KaFirType? ?: return null
            upperBoundForApproximationType.coneType
        } else {
            null
        }

        val coneType = ConeIntersectionType(conjunctConeTypes, upperBoundForApproximationConeType)
        return KaFirIntersectionType(coneType, session.firSymbolBuilder)
    }
}