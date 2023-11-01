/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.annotations

import org.jetbrains.kotlin.analysis.api.annotations.AnnotationUseSiteTargetFilter
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.annotations.KtEmptyAnnotationsList
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KtFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val builder: KtSymbolByFirBuilder,
) : KtAnnotationsList() {
    private val useSiteSession: FirSession get() = builder.rootSession
    override val token: KtLifetimeToken get() = builder.token

    override val annotations: List<KtAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            annotations(firCallableSymbol, builder, receiverParameter)
        }

    override val annotationInfos: List<KtAnnotationApplicationInfo>
        get() = withValidityAssertion {
            annotationInfos(firCallableSymbol, useSiteSession, receiverParameter)
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        hasAnnotation(firCallableSymbol, classId, useSiteTargetFilter, useSiteSession, receiverParameter)
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KtAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        annotationsByClassId(firCallableSymbol, classId, useSiteTargetFilter, builder, receiverParameter)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firCallableSymbol, useSiteSession, receiverParameter)
        }

    companion object {
        fun create(firCallableSymbol: FirCallableSymbol<*>, builder: KtSymbolByFirBuilder): KtAnnotationsList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KtEmptyAnnotationsList(builder.token)
            } else {
                KtFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, builder)
            }
        }
    }
}
