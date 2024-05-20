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
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.name.ClassId

internal class KaFirAnnotationListForReceiverParameter private constructor(
    private val firCallableSymbol: FirCallableSymbol<*>,
    private val receiverParameter: FirAnnotationContainer,
    private val builder: KaSymbolByFirBuilder,
) : KaAnnotationsList() {
    private val useSiteSession: FirSession get() = builder.rootSession
    override val token: KaLifetimeToken get() = builder.token

    override val annotations: List<KaAnnotationApplicationWithArgumentsInfo>
        get() = withValidityAssertion {
            annotations(firCallableSymbol, builder, receiverParameter)
        }

    override val annotationInfos: List<KaAnnotationApplicationInfo>
        get() = withValidityAssertion {
            annotationInfos(firCallableSymbol, useSiteSession, token, receiverParameter)
        }

    override fun hasAnnotation(classId: ClassId, useSiteTargetFilter: AnnotationUseSiteTargetFilter): Boolean = withValidityAssertion {
        hasAnnotation(firCallableSymbol, classId, useSiteTargetFilter, useSiteSession, receiverParameter)
    }

    override fun annotationsByClassId(
        classId: ClassId,
        useSiteTargetFilter: AnnotationUseSiteTargetFilter,
    ): List<KaAnnotationApplicationWithArgumentsInfo> = withValidityAssertion {
        annotationsByClassId(firCallableSymbol, classId, useSiteTargetFilter, builder, receiverParameter)
    }

    override val annotationClassIds: Collection<ClassId>
        get() = withValidityAssertion {
            annotationClassIds(firCallableSymbol, useSiteSession, receiverParameter)
        }

    companion object {
        fun create(firCallableSymbol: FirCallableSymbol<*>, builder: KaSymbolByFirBuilder): KaAnnotationsList {
            val receiverParameter = firCallableSymbol.receiverParameter
            return if (receiverParameter?.annotations?.isEmpty() != false) {
                KaEmptyAnnotationsList(builder.token)
            } else {
                KaFirAnnotationListForReceiverParameter(firCallableSymbol, receiverParameter, builder)
            }
        }
    }
}
