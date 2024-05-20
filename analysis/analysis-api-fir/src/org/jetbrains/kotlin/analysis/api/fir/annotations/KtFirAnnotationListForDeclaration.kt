/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KaEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val builder: KaSymbolByFirBuilder,
) : KaAnnotationsList() {
    override val token: KaLifetimeToken get() = builder.token
    private val useSiteSession: FirSession get() = builder.rootSession

    override val annotations: List<KaAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            annotations(firSymbol, builder)
        }

    override val annotationInfos: List<KaAnnotationApplicationInfo>
        get() = withValidityAssertion {
            annotationInfos(firSymbol, useSiteSession, token)
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        hasAnnotation(firSymbol, classId, useSiteTargetFilter, useSiteSession)
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        annotationsByClassId(firSymbol, classId, useSiteTargetFilter, builder)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firSymbol, useSiteSession)
        }

    companion object {
        fun create(firSymbol: FirBasedSymbol<*>, builder: KaSymbolByFirBuilder): KaAnnotationsList {
            return when {
                firSymbol is FirBackingFieldSymbol && firSymbol.propertySymbol.annotations.any { it.useSiteTarget == null } ->
                    KaFirAnnotationListForDeclaration(firSymbol, builder)
                firSymbol.annotations.isEmpty() ->
                    KaEmptyAnnotationsList(builder.token)
                else ->
                    KaFirAnnotationListForDeclaration(firSymbol, builder)
            }
        }
    }
}
