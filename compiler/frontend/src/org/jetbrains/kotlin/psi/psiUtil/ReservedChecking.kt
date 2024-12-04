/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.psiUtil

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

fun checkReservedYield(expression: KtSimpleNameExpression?, sink: DiagnosticSink) {
    // do not force identifier calculation for elements from stubs.
    if (expression?.getReferencedName() != "yield") return

    val identifier = expression.getIdentifier() ?: return

    if (identifier.node.elementType == KtTokens.IDENTIFIER && "yield" == identifier.text) {
        sink.report(Errors.YIELD_IS_RESERVED.on(identifier, "Identifier 'yield' is reserved. Use backticks to call it: `yield`"))
    }
}

val MESSAGE_FOR_YIELD_BEFORE_LAMBDA = "Reserved yield block/lambda. Use 'yield() { ... }' or 'yield(fun...)'"

fun checkReservedYieldBeforeLambda(element: PsiElement, sink: DiagnosticSink) {
    KtPsiUtil.getPreviousWord(element, "yield")?.let {
        sink.report(Errors.YIELD_IS_RESERVED.on(it, MESSAGE_FOR_YIELD_BEFORE_LAMBDA))
    }
}