/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val useSiteSession: FirSession,
    override val token: KtLifetimeToken,
) : KtAnnotationsList() {

    override val annotations: List<KtAnnotationApplication>
        get() = withValidityAssertion {
            annotations(firCallableSymbol, useSiteSession, receiverParameter)
        }

    override fun hasAnnotation(
        classId: ClassId,
        useSiteTarget: AnnotationUseSiteTarget?,
        acceptAnnotationsWithoutUseSite: Boolean,
    ): Boolean = withValidityAssertion {
        hasAnnotation(firCallableSymbol, classId, useSiteSession, useSiteTarget, acceptAnnotationsWithoutUseSite, receiverParameter)
    }

    override fun hasAnnotation(classId: ClassId): Boolean = withValidityAssertion {
        hasAnnotation(firCallableSymbol, classId, useSiteSession, receiverParameter)
    }

    override fun annotationsByClassId(classId: ClassId): List<KtAnnotationApplication> = withValidityAssertion {
        annotationsByClassId(firCallableSymbol, classId, useSiteSession, receiverParameter)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firCallableSymbol, receiverParameter)
        }

    companion object {
        fun create(
            firCallableSymbol: FirCallableSymbol<*>,
            useSiteSession: FirSession,
            token: KtLifetimeToken,
        ): KtAnnotationsList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KtEmptyAnnotationsList(token)
            } else {
                KtFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, useSiteSession, token)
            }
        }
    }
}
