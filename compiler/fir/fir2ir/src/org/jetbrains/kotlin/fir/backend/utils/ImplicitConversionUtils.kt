/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrImplicitCastInserter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.ir.expressions.IrExpression

fun IrExpression.prepareExpressionForGivenExpectedType(
    // TODO: Convert it to the context parameter at some moment
    c: Fir2IrComponents,
    expression: FirExpression,
    valueType: ConeKotlinType,
    expectedType: ConeKotlinType,
    // In most cases, it should be the same as `expectedType`.
    // Currently, it's only used for a case of a call argument to a generic function or for a call argument of a vararg parameter.
    // In that case, we need to preserve the original parameter type to generate proper nullability checks/assertions.
    // But generally for conversions/casts one should use `substitutedExpectedType`.
    substitutedExpectedType: ConeKotlinType = expectedType,
): IrExpression {
    val expressionWithCast = with(c.implicitCastInserter) {
        // The conversions happen later in the function
        @OptIn(Fir2IrImplicitCastInserter.NoConversionsExpected::class)
        insertSpecialCast(expression, valueType, expectedType)
    }

    return with(c.adapterGenerator) {
        val samFunctionType = getFunctionTypeForPossibleSamType(substitutedExpectedType) ?: substitutedExpectedType
        expressionWithCast.applySuspendConversionIfNeeded(expression, samFunctionType)
            .applySamConversionIfNeeded(expression)
    }
}
