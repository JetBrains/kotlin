/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.bodies

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaVariableInitializerRenderer {
    public fun renderInitializer(analysisSession: KaSession, symbol: KaVariableSymbol, printer: PrettyPrinter)

    public object NO_INITIALIZER : KaVariableInitializerRenderer {
        override fun renderInitializer(analysisSession: KaSession, symbol: KaVariableSymbol, printer: PrettyPrinter) {
        }
    }

    public object ONLY_CONST_VALUE_INITIALIZERS : KaVariableInitializerRenderer {
        override fun renderInitializer(analysisSession: KaSession, symbol: KaVariableSymbol, printer: PrettyPrinter) {
            //todo add initializer to KaVariableSymbol and render for it too KT-54794/
            val initializer = (symbol as? KaPropertySymbol)?.initializer as? KaConstantInitializerValue ?: return
            printer.append(" = ")
            printer.append(initializer.constant.renderAsKotlinConstant())
        }
    }
}

public typealias KtVariableInitializerRenderer = KaVariableInitializerRenderer