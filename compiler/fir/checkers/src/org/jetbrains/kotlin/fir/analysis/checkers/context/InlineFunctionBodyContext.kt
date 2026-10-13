/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.resolve.transformers.publishedApiEffectiveVisibility
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

class InlineFunctionBodyContext(
    val inlineFunction: FirFunctionSymbol<*>,
    val inlineFunEffectiveVisibility: EffectiveVisibility,
    val parentInlineContext: InlineFunctionBodyContext?,
)

fun createInlineFunctionBodyContext(
    function: FirFunctionSymbol<*>,
    parentInlineContext: InlineFunctionBodyContext?
): InlineFunctionBodyContext {
    return InlineFunctionBodyContext(
        function,
        function.publishedApiEffectiveVisibility ?: function.effectiveVisibility,
        parentInlineContext,
    )
}
