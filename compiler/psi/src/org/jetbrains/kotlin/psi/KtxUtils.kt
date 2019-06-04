package org.jetbrains.kotlin.psi

import com.intellij.psi.PsiElement

fun KtxLambdaExpression.replaceImpl(element: PsiElement): PsiElement {
    // KtxLambdaExpressions that have a single statement will have the same startOffset/endOffset as the
    // statement itself. This means that during refactoring, `replace(...)` may get called with an expression
    // that was actually meant to replace the first statement instead. In fact, this is almost always the case,
    // since there are no refactorings right now that would ever replace a KtxLambdaExpression directly. As a
    // result, we want to find the first statement in the current body and replace that instead, since that is actually
    // what is intended here.
    return when (element) {
        is KtxLambdaExpression -> replace(element)
        else -> {
            val body = functionLiteral.bodyExpression!!
            body.statements.first().replace(element)
            this
        }
    }
}