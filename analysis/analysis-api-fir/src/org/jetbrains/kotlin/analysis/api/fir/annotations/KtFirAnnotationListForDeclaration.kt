/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.toKtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForDeclaration private constructor(
    val firSymbol: FirBasedSymbol<*>,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {
    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            firSymbol.resolvedAnnotationsWithArguments
                .map { annotation ->
                    annotation.toKtAnnotationApplication(useSiteSession)
                }
        }

    override fun hasAnnotation(
        classId: ClassId,
        useSiteTarget: AnnotationUseSiteTarget?,
        acceptAnnotationsWithoutUseSite: Boolean,
    ): Boolean = withValidityAssertion {
        firSymbol.resolvedAnnotationsWithClassIds.any {
            (it.useSiteTarget == useSiteTarget || acceptAnnotationsWithoutUseSite && it.useSiteTarget == null) &&
                    it.toAnnotationClassId(useSiteSession) == classId
        }
    }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        firSymbol.resolvedAnnotationsWithClassIds.hasAnnotation(classId, useSiteSession)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        firSymbol.resolvedAnnotationsWithArguments.mapNotNull { annotation ->
            if (annotation.toAnnotationClassId(useSiteSession) != classId) return@mapNotNull null
            annotation.toKtAnnotationApplication(useSiteSession)
        }
    }


    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion { firSymbol.resolvedAnnotationClassIds }

    companion object {
        fun create(
            firSymbol: FirBasedSymbol<*>,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            return if (firSymbol.annotations.isEmpty()) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForDeclaration(firSymbol, useSiteSession, token)
            }
        }
    }
}
