/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

abstract class AbstractResultUnusedChecker(
    private val expressionChecker: (KtExpression) -> Boolean,
    private val callChecker: (ResolvedCall<*>) -> Boolean
) : AbstractKotlinInspection() {
    protected fun check(expression: KtExpression): Boolean {
        // Check whatever possible by PSI
        if (!expressionChecker(expression)) return false
        var parent: PsiElement? = expression.parent
        while (parent != null) {
            if (parent is KtBlockExpression || parent is KtFunction || parent is KtFile) break
            if (parent is KtValueArgument) return false
            if (parent is KtBinaryExpression && parent.operationToken in KtTokens.ALL_ASSIGNMENTS) return false
            parent = parent.parent
        }
        // Then check by call
        val context = expression.analyze(BodyResolveMode.PARTIAL_WITH_CFA)
        if (expression.isUsedAsExpression(context)) return false
        val resolvedCall = expression.getResolvedCall(context) ?: return false
        return callChecker(resolvedCall)
    }
}