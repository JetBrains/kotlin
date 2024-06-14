/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaExperimentalApi
public interface KaCallableReturnTypeFilter {
    public fun shouldRenderReturnType(analysisSession: KaSession, type: KaType, symbol: KaCallableSymbol): Boolean

    public object ALWAYS : KaCallableReturnTypeFilter {
        override fun shouldRenderReturnType(analysisSession: KaSession, type: KaType, symbol: KaCallableSymbol): Boolean {
            return true
        }

    }

    public object NO_UNIT_FOR_FUNCTIONS : KaCallableReturnTypeFilter {
        override fun shouldRenderReturnType(analysisSession: KaSession, type: KaType, symbol: KaCallableSymbol): Boolean {
            with(analysisSession) {
                return when (symbol) {
                    is KaFunctionSymbol -> !type.isUnitType
                    else -> true
                }
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaCallableReturnTypeFilter' instead", ReplaceWith("KaCallableReturnTypeFilter"))
public typealias KtCallableReturnTypeFilter = KaCallableReturnTypeFilter