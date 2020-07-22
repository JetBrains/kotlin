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
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.impl.FirFunctionCallImpl
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

object CanBeReplacedWithOperatorAssignmentChecker : FirExpressionChecker<FirVariableAssignment>() {
    override fun check(assignment: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        if (assignment.lValue !is FirResolvedNamedReference) return

        val left = assignment.lValue
        val right = assignment.rValue.psi as? KtBinaryExpression ?: return
        val rightFunctionCall = assignment.rValue as? FirFunctionCallImpl ?: return
        val rightTypeClassId = rightFunctionCall.explicitReceiver?.typeRef?.coneType?.classId

        if (rightTypeClassId !in StandardClassIds.primitiveTypes) return
        val rightResolvedSymbol = assignment.rValue.toResolvedCallableSymbol() ?: return
        if (rightResolvedSymbol.callableId.classId !in StandardClassIds.primitiveTypes) return

        if (right.matcher(left.psi as KtNameReferenceExpression)) {
            reporter.report(assignment.source, FirErrors.CAN_BE_REPLACED_WITH_OPERATOR_ASSIGNMENT)
        }
    }

    fun KtBinaryExpression.matcher(variable: KtNameReferenceExpression): Boolean {
        if ((left as? KtNameReferenceExpression)?.getReferencedName() == variable.getReferencedName()) return true
        if ((right as? KtNameReferenceExpression)?.getReferencedName() == variable.getReferencedName() && isCommutative()) return true

        return if (isCommutative()) {
            val leftExpression = left as? KtBinaryExpression
            val rightExpression = right as? KtBinaryExpression

            val isLeftMatch = isHierarchicallyTrue(operationToken, leftExpression?.operationToken)
                    && leftExpression?.matcher(variable) ?: false
            val isRightMatch = isHierarchicallyTrue(operationToken, rightExpression?.operationToken)
                    && rightExpression?.matcher(variable) ?: false
            isLeftMatch or isRightMatch
        } else {
            val leftExpression = left as? KtBinaryExpression

            isHierarchicallyTrue(operationToken, leftExpression?.operationToken)
                    && leftExpression?.matcher(variable) ?: false
        }
    }

    private fun KtBinaryExpression.isCommutative() = this.operationToken == KtTokens.PLUS || this.operationToken == KtTokens.MUL

    private fun isHierarchicallyTrue(currentOperation: IElementType, nextOperation: IElementType?) = currentOperation == nextOperation
}