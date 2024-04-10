/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtFunctionLikeBodyRenderer {
    public fun renderBody(analysisSession: KtAnalysisSession, symbol: KtFunctionLikeSymbol, printer: PrettyPrinter)

    public object NO_BODY : KtFunctionLikeBodyRenderer {
        override fun renderBody(analysisSession: KtAnalysisSession, symbol: KtFunctionLikeSymbol, printer: PrettyPrinter) {}
    }
}

