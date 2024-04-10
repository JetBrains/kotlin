/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtVariableInitializerRenderer {
    public fun renderInitializer(analysisSession: KtAnalysisSession, symbol: KtVariableSymbol, printer: PrettyPrinter)

    public object NO_INITIALIZER : KtVariableInitializerRenderer {
        override fun renderInitializer(analysisSession: KtAnalysisSession, symbol: KtVariableSymbol, printer: PrettyPrinter) {
        }
    }

    public object ONLY_CONST_VALUE_INITIALIZERS : KtVariableInitializerRenderer {
        override fun renderInitializer(analysisSession: KtAnalysisSession, symbol: KtVariableSymbol, printer: PrettyPrinter) {
            //todo add initializer to KtVariableSymbol and render for it too KT-54794/
            val initializer = (symbol as? KtPropertySymbol)?.initializer as? KtConstantInitializerValue ?: return
            printer.append(" = ")
            printer.append(initializer.constant.renderAsKotlinConstant())
        }
    }
}

