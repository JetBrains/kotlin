/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class InlinableParameterContext(
    val inlineFunction: FirFunctionSymbol<*>,
)

fun createInlinableParameterContext(
    function: FirFunctionSymbol<*>,
): InlinableParameterContext {
    return InlinableParameterContext(function)
}
