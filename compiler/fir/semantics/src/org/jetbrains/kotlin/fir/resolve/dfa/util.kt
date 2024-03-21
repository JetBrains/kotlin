/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeContext

fun TypeStatement?.smartCastedType(context: ConeTypeContext, originalType: ConeKotlinType): ConeKotlinType =
    if (this != null && exactType.isNotEmpty()) {
        context.intersectTypes(exactType.toMutableList().also { it += originalType })
    } else {
        originalType
    }

@DfaInternals
fun FirOperation.isEq(): Boolean {
    return when (this) {
        FirOperation.EQ, FirOperation.IDENTITY -> true
        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
        else -> throw IllegalArgumentException("$this should not be there")
    }
}

@DfaInternals
fun FirElement.unwrapElement(): FirElement = when (this) {
    is FirWhenSubjectExpression -> whenRef.value.let { it.subjectVariable ?: it.subject }?.unwrapElement() ?: this
    is FirSmartCastExpression -> originalExpression.unwrapElement()
    is FirSafeCallExpression -> selector.unwrapElement()
    is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
    is FirCheckNotNullCall -> argument.unwrapElement()
    is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
    is FirVariableAssignment -> lValue.unwrapElement()
    else -> this
}
