/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

@KaExperimentalApi
public interface KaParameterDefaultValueRenderer {
    public fun renderDefaultValue(analysisSession: KaSession, symbol: KaValueParameterSymbol, printer: PrettyPrinter)

    public object NO_DEFAULT_VALUE : KaParameterDefaultValueRenderer {
        override fun renderDefaultValue(analysisSession: KaSession, symbol: KaValueParameterSymbol, printer: PrettyPrinter) {
        }
    }

    public object THREE_DOTS : KaParameterDefaultValueRenderer {
        override fun renderDefaultValue(analysisSession: KaSession, symbol: KaValueParameterSymbol, printer: PrettyPrinter) {
            if (symbol.hasDefaultValue) {
                printer.append("...")
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaParameterDefaultValueRenderer' instead", ReplaceWith("KaParameterDefaultValueRenderer"))
public typealias KtParameterDefaultValueRenderer = KaParameterDefaultValueRenderer