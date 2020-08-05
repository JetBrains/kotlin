/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression

object CanBeReplacedWithOperatorAssignmentChecker : FirExpressionChecker<FirVariableAssignment>() {
    override fun check(assignment: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val lValue = assignment.lValue
        if (lValue !is FirResolvedNamedReference) return

        val operator = assignment.psi?.children?.getOrNull(1) ?: return
        val operationSign = (operator as? KtOperationReferenceExpression)?.operationSignTokenType
        if (operationSign != KtTokens.EQ) return

        val lValuePsi = lValue.psi as? KtNameReferenceExpression ?: return
        val rValue = assignment.rValue as? FirFunctionCall ?: return
        val rValuePsi = rValue.psi as? KtBinaryExpression ?: return
        val rValueClassId = rValue.explicitReceiver?.typeRef?.coneType?.classId

        if (rValueClassId !in StandardClassIds.primitiveTypes) return
        val rValueResolvedSymbol = rValue.toResolvedCallableSymbol() ?: return
        if (rValueResolvedSymbol.callableId.classId !in StandardClassIds.primitiveTypes) return

        if (rValuePsi.matcher(lValuePsi)) {
            val operatorStatement = operator.toFirPsiSourceElement()
            reporter.report(operatorStatement, FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT)
        }
    }

    fun KtBinaryExpression.matcher(variable: KtNameReferenceExpression): Boolean {
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
}