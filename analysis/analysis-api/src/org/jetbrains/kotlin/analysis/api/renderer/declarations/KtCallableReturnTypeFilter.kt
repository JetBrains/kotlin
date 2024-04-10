/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

public interface KtCallableReturnTypeFilter {
    public fun shouldRenderReturnType(analysisSession: KtAnalysisSession, type: KtType, symbol: KtCallableSymbol): Boolean

    public object ALWAYS : KtCallableReturnTypeFilter {
        override fun shouldRenderReturnType(analysisSession: KtAnalysisSession, type: KtType, symbol: KtCallableSymbol): Boolean {
            return true
        }

    }

    public object NO_UNIT_FOR_FUNCTIONS : KtCallableReturnTypeFilter {
        override fun shouldRenderReturnType(analysisSession: KtAnalysisSession, type: KtType, symbol: KtCallableSymbol): Boolean {
            with(analysisSession) {
                return when (symbol) {
                    is KtFunctionSymbol -> !type.isUnit
                    else -> true
                }
            }
        }
    }
}