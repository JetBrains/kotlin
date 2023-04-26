/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            annotations(firSymbol, useSiteSession)
        }

    override val annotationInfos: List<KtAnnotationApplicationInfo>
        get() = withValidityAssertion {
            annotationInfos(firSymbol, useSiteSession)
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        hasAnnotation(firSymbol, classId, useSiteTargetFilter, useSiteSession)
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KtAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        annotationsByClassId(firSymbol, classId, useSiteTargetFilter, useSiteSession)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firSymbol, useSiteSession)
        }

    companion object {
        fun create(
            firSymbol: FirBasedSymbol<*>,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            return when {
                firSymbol is FirBackingFieldSymbol && firSymbol.propertySymbol.annotations.any { it.useSiteTarget == null } ->
                    KtFirAnnotationListForDeclaration(firSymbol, useSiteSession, token)
                firSymbol.annotations.isEmpty() ->
                    KtEmptyAnnotationsList(token)
                else ->
                    KtFirAnnotationListForDeclaration(firSymbol, useSiteSession, token)
            }
        }
    }
}
