/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtParameterDefaultValueRenderer {
    public fun renderDefaultValue(analysisSession: KtAnalysisSession, symbol: KtValueParameterSymbol, printer: PrettyPrinter)

    public object NO_DEFAULT_VALUE : KtParameterDefaultValueRenderer {
        override fun renderDefaultValue(analysisSession: KtAnalysisSession, symbol: KtValueParameterSymbol, printer: PrettyPrinter) {
        }
    }

    public object THREE_DOTS : KtParameterDefaultValueRenderer {
        override fun renderDefaultValue(analysisSession: KtAnalysisSession, symbol: KtValueParameterSymbol, printer: PrettyPrinter) {
            if (symbol.hasDefaultValue) {
                printer.append("...")
            }
        }
    }
}

