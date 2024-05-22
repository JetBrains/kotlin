/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.calls.KaCompoundAccess
import org.jetbrains.kotlin.analysis.api.calls.KaExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.components.KaResolver
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

@KaAnalysisApiInternals
abstract class KaAbstractResolver : KaResolver() {
    protected fun KtBinaryExpression.getCompoundAssignKind(): KaCompoundAccess.CompoundAssign.Kind {
        val compoundAssignKind = when (operationToken) {
            KtTokens.PLUSEQ -> KaCompoundAccess.CompoundAssign.Kind.PLUS_ASSIGN
            KtTokens.MINUSEQ -> KaCompoundAccess.CompoundAssign.Kind.MINUS_ASSIGN
            KtTokens.MULTEQ -> KaCompoundAccess.CompoundAssign.Kind.TIMES_ASSIGN
            KtTokens.PERCEQ -> KaCompoundAccess.CompoundAssign.Kind.REM_ASSIGN
            KtTokens.DIVEQ -> KaCompoundAccess.CompoundAssign.Kind.DIV_ASSIGN
            else -> error("unexpected operator $operationToken")
        }
        return compoundAssignKind
    }

    protected fun KtUnaryExpression.getInOrDecOperationKind(): KaCompoundAccess.IncOrDecOperation.Kind {
        val incOrDecOperationKind = when (operationToken) {
            KtTokens.PLUSPLUS -> KaCompoundAccess.IncOrDecOperation.Kind.INC
            KtTokens.MINUSMINUS -> KaCompoundAccess.IncOrDecOperation.Kind.DEC
            else -> error("unexpected operator $operationToken")
        }
        return incOrDecOperationKind
    }

    protected fun KtExpression.toExplicitReceiverValue(type: KaType): KaExplicitReceiverValue =
        KaExplicitReceiverValue(this, type, isReceiverOfKtSafeQualifiedExpression(), token)

    private fun KtExpression.isReceiverOfKtSafeQualifiedExpression(): Boolean {
        val safeQualifiedExpression = parentOfType<KtSafeQualifiedExpression>() ?: return false
        return KtPsiUtil.deparenthesize(safeQualifiedExpression.receiverExpression) == KtPsiUtil.deparenthesize(this)
    }

    protected fun canBeResolvedAsCall(ktElement: KtElement): Boolean = when (ktElement) {
        is KtBinaryExpression -> ktElement.operationToken !in nonCallBinaryOperator
        is KtOperationReferenceExpression -> ktElement.operationSignTokenType !in nonCallBinaryOperator
        is KtCallElement -> true
        is KtConstructorCalleeExpression -> true
        is KtDotQualifiedExpression -> true
        is KtNameReferenceExpression -> true
        is KtOperationExpression -> true
        is KtArrayAccessExpression -> true
        is KtCallableReferenceExpression -> true
        is KtWhenConditionInRange -> true
        else -> false
    }

    protected companion object {
        private val nonCallBinaryOperator: Set<KtSingleValueToken> = setOf(KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)
    }
}