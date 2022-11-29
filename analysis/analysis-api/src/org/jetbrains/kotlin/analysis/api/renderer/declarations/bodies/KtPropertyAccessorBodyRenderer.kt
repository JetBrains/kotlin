/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtPropertyAccessorBodyRenderer {
    context(KtAnalysisSession)
    public fun renderBody(symbol: KtPropertyAccessorSymbol, printer: PrettyPrinter)

    public object NO_BODY : KtPropertyAccessorBodyRenderer {
        context(KtAnalysisSession)
        override fun renderBody(symbol: KtPropertyAccessorSymbol, printer: PrettyPrinter) {
        }
    }
}

