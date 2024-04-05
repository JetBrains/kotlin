/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.types.Variance

public interface KtRendererTypeApproximator {
    public fun approximateType(analysisSession: KtAnalysisSession, type: KtType, position: Variance): KtType

    public object TO_DENOTABLE : KtRendererTypeApproximator {
        override fun approximateType(analysisSession: KtAnalysisSession, type: KtType, position: Variance): KtType {
            with(analysisSession) {
                val effectiveType = type.getEnhancedType() ?: type

                return when (position) {
                    Variance.INVARIANT -> effectiveType
                    Variance.IN_VARIANCE -> effectiveType.approximateToSubPublicDenotableOrSelf(approximateLocalTypes = false)
                    Variance.OUT_VARIANCE -> effectiveType.approximateToSuperPublicDenotableOrSelf(approximateLocalTypes = false)
                }
            }
        }
    }

    public object NO_APPROXIMATION : KtRendererTypeApproximator {
        override fun approximateType(analysisSession: KtAnalysisSession, type: KtType, position: Variance): KtType {
            return type
        }
    }
}
