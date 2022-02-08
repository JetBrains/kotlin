/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val useSiteSession: FirSession,
    override val token: ValidityToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            firSymbol.resolvedAnnotationsWithArguments
                .map { annotation ->
                    annotation.toKtAnnotationApplication(useSiteSession)
                }
        }


    override fun containsAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        firSymbol.resolvedAnnotationClassIds.contains(classId)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        firSymbol.resolvedAnnotationsWithArguments.mapNotNull { annotation ->
            if (annotation.fullyExpandedClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    }


    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { firSymbol.resolvedAnnotationClassIds }

    companion object {
        fun create(
            firSymbol: FirBasedSymbol<*>,
            useSiteSession: FirSession,
            token: ValidityToken,
        ): KtAnnotationsList {
            return if (firSymbol.annotations.isEmpty()) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForDeclaration(firSymbol, useSiteSession, token)
            }
        }
    }
}
