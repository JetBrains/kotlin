/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

@KaExperimentalApi
public interface KaRendererTypeApproximator {
    public fun approximateType(analysisSession: KaSession, type: KaType, position: Variance): KaType

    public object TO_DENOTABLE : KaRendererTypeApproximator {
        @OptIn(KaExperimentalApi::class)
        override fun approximateType(analysisSession: KaSession, type: KaType, position: Variance): KaType {
            with(analysisSession) {
                val effectiveType = type.enhancedType ?: type

                return when (position) {
                    Variance.INVARIANT -> effectiveType
                    Variance.IN_VARIANCE -> effectiveType.approximateToSubPublicDenotableOrSelf(approximateLocalTypes = false)
                    Variance.OUT_VARIANCE -> effectiveType.approximateToSuperPublicDenotableOrSelf(approximateLocalTypes = false)
                }
            }
        }
    }

    public object NO_APPROXIMATION : KaRendererTypeApproximator {
        override fun approximateType(analysisSession: KaSession, type: KaType, position: Variance): KaType {
            return type
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaRendererTypeApproximator' instead", ReplaceWith("KaRendererTypeApproximator"))
public typealias KtRendererTypeApproximator = KaRendererTypeApproximator