/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

object CanBeReplacedWithOperatorAssignmentChecker : FirExpressionChecker<FirVariableAssignment>() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val lValue = expression.lValue
        if (lValue !is FirResolvedNamedReference) return
        if (expression.source?.kind is FirFakeSourceElementKind) return

        val rValue = expression.rValue as? FirFunctionCall ?: return
        if (rValue.source?.kind is FirFakeSourceElementKind) return

        val rValueClassId = rValue.explicitReceiver?.typeRef?.coneType?.classId
        if (rValueClassId !in StandardClassIds.primitiveTypes) return
        val rValueResolvedSymbol = rValue.toResolvedCallableSymbol() ?: return
        if (rValueResolvedSymbol.dispatchReceiverClassOrNull()?.classId !in StandardClassIds.primitiveTypes) return

        var needToReport = false
        val assignmentSource = expression.source

        if (assignmentSource is FirPsiSourceElement<*>) {
            val lValuePsi = lValue.psi as? KtNameReferenceExpression ?: return
            val rValuePsi = rValue.psi as? KtBinaryExpression ?: return

            if (rValuePsi.matcher(lValuePsi)) {
                needToReport = true
            }
        } else if (assignmentSource is FirLightSourceElement) {
            val lValueLightTree = lValue.source!!.lighterASTNode
            val rValueLightTree = rValue.source!!.lighterASTNode
            if (lightTreeMatcher(lValueLightTree, rValueLightTree, assignmentSource)) {
                needToReport = true
            }
        }

        if (needToReport) {
            val source = expression.source?.getChild(setOf(KtTokens.EQ))
            reporter.report(source, FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT)
        }

    }

    fun lightTreeMatcher(
        variable: LighterASTNode,
        expression: LighterASTNode,
        source: FirLightSourceElement,
        prevOperator: LighterASTNode? = null
    ): Boolean {
        val tree = source.treeStructure
        val children = expression.getChildren(tree).filterNotNull()

        val operator = children.firstOrNull { it.tokenType == KtNodeTypes.OPERATION_REFERENCE }
        if (prevOperator != null && !isLightNodesHierarchicallyTrue(prevOperator, operator)) return false
        if (operator?.canBeAugmented() == false) return false

        val commutative = operator != null && isCommutativeOperator(operator)
        var afterOperatorNode = false
        children.forEach {
            when (it.tokenType) {
                KtNodeTypes.REFERENCE_EXPRESSION -> {
                    if ((commutative || !afterOperatorNode) && it.toString() == variable.toString()) return true
                }
                KtNodeTypes.BINARY_EXPRESSION -> {
                    return lightTreeMatcher(variable, it, source, operator)
                }
                KtNodeTypes.OPERATION_REFERENCE -> {
                    afterOperatorNode = true
                }
            }
        }
        return false
    }

    private fun KtBinaryExpression.matcher(variable: KtNameReferenceExpression): Boolean {
        if (!canBeAugmented()) return false
        if ((left as? KtNameReferenceExpression)?.getReferencedName() == variable.getReferencedName()) return true
        if ((right as? KtNameReferenceExpression)?.getReferencedName() == variable.getReferencedName() && isCommutative()) return true

        return if (isCommutative()) {
            val leftExpression = left as? KtBinaryExpression
            val rightExpression = right as? KtBinaryExpression

            val isLeftMatch = isHierarchicallyTrue(operationToken, leftExpression?.operationToken)
                    && leftExpression?.matcher(variable) == true
            if (isLeftMatch) return true
            val isRightMatch = isHierarchicallyTrue(operationToken, rightExpression?.operationToken)
                    && rightExpression?.matcher(variable) == true
            if (isRightMatch) return true

            false
        } else {
            val leftExpression = left as? KtBinaryExpression

            isHierarchicallyTrue(operationToken, leftExpression?.operationToken)
                    && leftExpression?.matcher(variable) == true
        }
    }

    private fun KtBinaryExpression.isCommutative() = this.operationToken == KtTokens.PLUS || this.operationToken == KtTokens.MUL

    private fun KtBinaryExpression.canBeAugmented() = this.operationToken == KtTokens.PLUS
            || this.operationToken == KtTokens.MUL
            || this.operationToken == KtTokens.MINUS
            || this.operationToken == KtTokens.DIV
            || this.operationToken == KtTokens.PERC

    private fun isHierarchicallyTrue(currentOperation: IElementType, nextOperation: IElementType?) = currentOperation == nextOperation

    private fun isCommutativeOperator(operator: LighterASTNode) = operator.toString().let { it == "+" || it == "*" }

    private fun isLightNodesHierarchicallyTrue(first: LighterASTNode?, second: LighterASTNode?) =
        first.toString() == second.toString()

    private fun LighterASTNode.canBeAugmented() = this.toString().let {
        it == "+" || it == "*" || it == "-" || it == "/" || it == "%"
    }
}
