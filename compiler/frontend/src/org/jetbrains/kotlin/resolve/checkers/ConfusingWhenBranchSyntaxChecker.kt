/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.Errors.CONFUSING_BRANCH_CONDITION
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingTrace

object ConfusingWhenBranchSyntaxChecker {
    private val prohibitedTokens = TokenSet.create(
        IN_KEYWORD, NOT_IN,
        LT, LTEQ, GT, GTEQ,
        EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ,
        ANDAND, OROR
    )

    fun check(whenExpression: KtWhenExpression, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        if (whenExpression.subjectExpression == null && whenExpression.subjectVariable == null) return
        for (entry in whenExpression.entries) {
            for (condition in entry.conditions) {
                checkCondition(condition, languageVersionSettings, trace)
            }
        }
    }

    private fun checkCondition(condition: KtWhenCondition, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        when (condition) {
            is KtWhenConditionWithExpression -> checkConditionExpression(condition.expression, languageVersionSettings, trace)
            is KtWhenConditionInRange -> checkConditionExpression(condition.rangeExpression, languageVersionSettings, trace)
        }
    }

    private fun checkConditionExpression(rawExpression: KtExpression?, languageVersionSettings: LanguageVersionSettings, trace: BindingTrace) {
        if (rawExpression == null) return
        if (rawExpression is KtParenthesizedExpression) return
        val shouldReport = when (val expression = KtPsiUtil.safeDeparenthesize(rawExpression)) {
            is KtIsExpression -> true
            is KtBinaryExpression -> expression.operationToken in prohibitedTokens
            else -> false
        }
        if (shouldReport) {
            trace.report(CONFUSING_BRANCH_CONDITION.on(languageVersionSettings, rawExpression))
        }
    }
}
