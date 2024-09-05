/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.ElementTypeUtils.getOperationSymbol
import org.jetbrains.kotlin.ElementTypeUtils.isExpression
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirBooleanOperatorExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.util.getChildren

object FirConfusingWhenBranchSyntaxChecker : FirExpressionSyntaxChecker<FirWhenExpression, PsiElement>() {
    private val prohibitedTokens = TokenSet.create(
        IN_KEYWORD, NOT_IN,
        LT, LTEQ, GT, GTEQ,
        EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ,
        ANDAND, OROR
    )

    override fun checkLightTree(
        element: FirWhenExpression,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val subjectType = element.subject?.resolvedType ?: element.subjectVariable?.returnTypeRef?.coneTypeOrNull ?: return
        val tree = source.treeStructure
        val entries = source.lighterASTNode.getChildren(tree).filter { it.tokenType == WHEN_ENTRY }
        val offset = source.startOffset - source.lighterASTNode.startOffset
        for (entry in entries) {
            for (node in entry.getChildren(tree)) {
                when (node.tokenType) {
                    WHEN_CONDITION_EXPRESSION -> {
                        val lightTreeNode = node.getChildren(tree).firstOrNull { it.isExpression() } ?: continue
                        val firCondition = element.branches.find { it.source?.lighterASTNode == entry }?.condition
                        val firPattern = (firCondition as? FirEqualityOperatorCall)?.arguments?.getOrNull(1)
                        checkConditionExpression(offset, lightTreeNode, subjectType, firPattern, tree, context, reporter)
                    }
                    WHEN_CONDITION_IN_RANGE -> {
                        val lightTreeNode =
                            node.getChildren(tree).firstOrNull { it.tokenType != OPERATION_REFERENCE && it.isExpression() } ?: continue
                        checkConditionExpression(offset, lightTreeNode, subjectType, null, tree, context, reporter)
                    }
                }
            }
        }
    }

    private fun checkConditionExpression(
        offset: Int,
        expression: LighterASTNode,
        subjectType: ConeKotlinType,
        firPattern: FirExpression?,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val (shouldReport, potentialGuardLhs) = when (expression.tokenType) {
            IS_EXPRESSION -> true to null
            BINARY_EXPRESSION -> {
                val operationTokenName = expression.getChildren(tree).first { it.tokenType == OPERATION_REFERENCE }.toString()
                val operationToken = operationTokenName.getOperationSymbol()
                val shouldReport = operationToken in prohibitedTokens
                val potentialGuardLhs = (firPattern as? FirBooleanOperatorExpression)?.leftOperand?.takeIf { operationToken == ANDAND }
                shouldReport to potentialGuardLhs
            }
            else -> false to null
        }
        if (shouldReport) {
            val source =
                KtLightSourceElement(expression, offset + expression.startOffset, offset + expression.endOffset, tree)
            if (potentialGuardLhs != null && !subjectType.isBoolean && !potentialGuardLhs.resolvedType.isBoolean) {
                reporter.reportOn(source, FirErrors.WRONG_CONDITION_SUGGEST_GUARD, context)
            } else {
                reporter.reportOn(source, FirErrors.CONFUSING_BRANCH_CONDITION, context)
            }
        }
    }

    override fun checkPsi(
        element: FirWhenExpression,
        source: KtPsiSourceElement,
        psi: PsiElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val subjectType = element.subject?.resolvedType ?: element.subjectVariable?.returnTypeRef?.coneTypeOrNull ?: return
        val whenExpression = psi as KtWhenExpression
        if (whenExpression.subjectExpression == null && whenExpression.subjectVariable == null) return
        for (entry in whenExpression.entries) {
            val firCondition = element.branches.firstOrNull { it.source.psi == entry }?.condition.takeIf {
                entry.conditions.size == 1  // do not consider the case with multiple conditions
            }
            for (condition in entry.conditions) {
                checkCondition(condition, subjectType, firCondition, context, reporter)
            }
        }
    }

    private fun checkCondition(
        condition: KtWhenCondition,
        subjectType: ConeKotlinType,
        firCondition: FirExpression?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        when (condition) {
            is KtWhenConditionWithExpression -> {
                val firPattern = (firCondition as? FirEqualityOperatorCall)?.arguments?.getOrNull(1)
                checkConditionExpression(condition.expression, subjectType, firPattern, context, reporter)
            }
            is KtWhenConditionInRange -> checkConditionExpression(condition.rangeExpression, subjectType, null, context, reporter)
        }
    }

    private fun checkConditionExpression(
        rawExpression: KtExpression?,
        subjectType: ConeKotlinType,
        firPattern: FirExpression?,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (rawExpression == null) return
        if (rawExpression is KtParenthesizedExpression) return
        val (shouldReport, potentialGuardLhs) = when (val expression = KtPsiUtil.safeDeparenthesize(rawExpression)) {
            is KtIsExpression -> true to null
            is KtBinaryExpression -> {
                val shouldReport = expression.operationToken in prohibitedTokens
                val potentialGuardLhs =
                    (firPattern as? FirBooleanOperatorExpression)?.leftOperand?.takeIf { expression.operationToken == ANDAND }
                shouldReport to potentialGuardLhs
            }
            else -> false to null
        }
        if (shouldReport) {
            val source = KtRealPsiSourceElement(rawExpression)
            if (potentialGuardLhs != null && !subjectType.isBoolean && !potentialGuardLhs.resolvedType.isBoolean) {
                reporter.reportOn(source, FirErrors.WRONG_CONDITION_SUGGEST_GUARD, context)
            } else {
                reporter.reportOn(source, FirErrors.CONFUSING_BRANCH_CONDITION, context)
            }
        }
    }
}
