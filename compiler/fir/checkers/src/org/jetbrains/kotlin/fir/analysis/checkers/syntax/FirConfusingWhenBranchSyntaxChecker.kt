/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.ElementTypeUtils.getOperationSymbol
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isBooleanOrNullableBoolean
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.getChildren

object FirConfusingWhenBranchSyntaxChecker : FirExpressionSyntaxChecker<FirWhenExpression, PsiElement>() {
    override fun checkLightTree(
        element: FirWhenExpression,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val subjectType = element.subjectVariable?.initializer?.resolvedType ?: element.subjectVariable?.returnTypeRef?.coneType ?: return
        val booleanSubject = subjectType.isBooleanOrNullableBoolean
        val tree = source.treeStructure
        val entries = source.lighterASTNode.getChildren(tree).filter { it.tokenType == WHEN_ENTRY }
        val offset = source.startOffset - source.lighterASTNode.startOffset
        for (entry in entries) {
            for (node in entry.getChildren(tree)) {
                val expression = when (node.tokenType) {
                    WHEN_CONDITION_EXPRESSION -> node.getChildren(tree).firstOrNull { it.isExpression() }
                    WHEN_CONDITION_IN_RANGE -> node.getChildren(tree)
                        .firstOrNull { it.tokenType != OPERATION_REFERENCE && it.isExpression() }
                    else -> null
                } ?: continue
                checkConditionExpression(booleanSubject, offset, expression, tree, context, reporter)
            }
        }
    }

    private fun checkConditionExpression(
        booleanSubject: Boolean,
        offset: Int,
        expression: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val errorReporter = when (expression.tokenType) {
            IS_EXPRESSION -> ConfusingWhenBranchReporter.Generic
            BINARY_EXPRESSION -> {
                val operationTokenName = expression.getChildren(tree).first { it.tokenType == OPERATION_REFERENCE }.toString()
                val operationToken = operationTokenName.getOperationSymbol()
                ConfusingWhenBranchReporter(operationToken, booleanSubject)
            }
            else -> null
        } ?: return
        val source = KtLightSourceElement(expression, offset + expression.startOffset, offset + expression.endOffset, tree)
        errorReporter.report(reporter, source, context)
    }

    override fun checkPsi(
        element: FirWhenExpression,
        source: KtPsiSourceElement,
        psi: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val subjectType = element.subjectVariable?.initializer?.resolvedType ?: element.subjectVariable?.returnTypeRef?.coneType ?: return
        val booleanSubject = subjectType.isBooleanOrNullableBoolean
        val whenExpression = psi as KtWhenExpression
        if (whenExpression.subjectExpression == null && whenExpression.subjectVariable == null) return
        for (entry in whenExpression.entries) {
            for (condition in entry.conditions) {
                checkCondition(booleanSubject, condition, context, reporter)
            }
        }
    }

    private fun checkCondition(booleanSubject: Boolean, condition: KtWhenCondition, context: CheckerContext, reporter: DiagnosticReporter) {
        when (condition) {
            is KtWhenConditionWithExpression -> checkConditionExpression(booleanSubject, condition.expression, context, reporter)
            is KtWhenConditionInRange -> checkConditionExpression(booleanSubject, condition.rangeExpression, context, reporter)
        }
    }

    private fun checkConditionExpression(
        booleanSubject: Boolean,
        rawExpression: KtExpression?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (rawExpression == null) return
        if (rawExpression is KtParenthesizedExpression) return
        val errorReporter = when (val expression = KtPsiUtil.safeDeparenthesize(rawExpression)) {
            is KtIsExpression -> ConfusingWhenBranchReporter.Generic
            is KtBinaryExpression -> ConfusingWhenBranchReporter(expression.operationToken, booleanSubject)
            else -> null
        } ?: return
        val source = KtRealPsiSourceElement(rawExpression)
        errorReporter.report(reporter, source, context)
    }

    private fun interface ConfusingWhenBranchReporter {
        fun report(reporter: DiagnosticReporter, source: AbstractKtSourceElement?, context: DiagnosticContext)

        companion object {
            private val prohibitedTokens = TokenSet.create(
                IN_KEYWORD, NOT_IN,
                LT, LTEQ, GT, GTEQ,
                EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ,
                ANDAND, OROR
            )

            operator fun invoke(operationToken: IElementType, booleanSubject: Boolean): ConfusingWhenBranchReporter? = when {
                operationToken == ANDAND && !booleanSubject -> GuardSuggestion
                operationToken in prohibitedTokens -> Generic
                else -> null
            }

            val Generic: ConfusingWhenBranchReporter = ConfusingWhenBranchReporter { reporter, source, context ->
                reporter.reportOn(source, FirErrors.CONFUSING_BRANCH_CONDITION, context)
            }

            val GuardSuggestion: ConfusingWhenBranchReporter = ConfusingWhenBranchReporter { reporter, source, context ->
                reporter.reportOn(source, FirErrors.WRONG_CONDITION_SUGGEST_GUARD, context)
            }
        }
    }
}