/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.calls.KtCompoundAccess
import org.jetbrains.kotlin.analysis.api.calls.KtExplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.components.KtCallResolver
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

abstract class AbstractKtCallResolver : KtCallResolver() {

    protected fun KtBinaryExpression.getCompoundAssignKind(): KtCompoundAccess.CompoundAssign.Kind {
        val compoundAssignKind = when (operationToken) {
            KtTokens.PLUSEQ -> KtCompoundAccess.CompoundAssign.Kind.PLUS_ASSIGN
            KtTokens.MINUSEQ -> KtCompoundAccess.CompoundAssign.Kind.MINUS_ASSIGN
            KtTokens.MULTEQ -> KtCompoundAccess.CompoundAssign.Kind.TIMES_ASSIGN
            KtTokens.PERCEQ -> KtCompoundAccess.CompoundAssign.Kind.REM_ASSIGN
            KtTokens.DIVEQ -> KtCompoundAccess.CompoundAssign.Kind.DIV_ASSIGN
            else -> error("unexpected operator $operationToken")
        }
        return compoundAssignKind
    }

    protected fun KtUnaryExpression.getInOrDecOperationKind(): KtCompoundAccess.IncOrDecOperation.Kind {
        val incOrDecOperationKind = when (operationToken) {
            KtTokens.PLUSPLUS -> KtCompoundAccess.IncOrDecOperation.Kind.INC
            KtTokens.MINUSMINUS -> KtCompoundAccess.IncOrDecOperation.Kind.DEC
            else -> error("unexpected operator $operationToken")
        }
        return incOrDecOperationKind
    }

    protected fun KtExpression.toExplicitReceiverValue(type: KtType): KtExplicitReceiverValue =
        KtExplicitReceiverValue(this, type, isReceiverOfKtSafeQualifiedExpression(), token)

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
        else -> false
    }

    protected companion object {
        private val nonCallBinaryOperator: Set<KtSingleValueToken> = setOf(KtTokens.ELVIS, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)
    }
}