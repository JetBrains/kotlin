/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrImplicitCastInserter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.render

context(c: Fir2IrComponents)
fun IrExpression.prepareExpressionForGivenExpectedType(
    expression: FirExpression,
    valueType: ConeKotlinType = expression.resolvedType.fullyExpandedType(),
    expectedType: ConeKotlinType,
    // In most cases, it should be the same as `expectedType`.
    // Currently, it's only used for a case of a call argument to a generic function or for a call argument of a vararg parameter.
    // In that case, we need to preserve the original parameter type to generate proper nullability checks/assertions.
    // But generally for conversions/casts one should use `substitutedExpectedType`.
    substitutedExpectedType: ConeKotlinType = expectedType,
    forReceiver: Boolean,
): IrExpression {
    if (this is IrVararg) {
        return applyConversionOnVararg(expression) {
            prepareExpressionForGivenExpectedType(expression = it, expectedType = substitutedExpectedType, forReceiver = forReceiver)
        }
    }

    @OptIn(Fir2IrImplicitCastInserter.NoConversionsExpected::class)
    val expressionWithCast = with(c.implicitCastInserter) {
        if (forReceiver) {
            insertCastForReceiver(valueType, substitutedExpectedType)
        } else {
            insertCastForIntersectionTypeOrSelf(valueType, substitutedExpectedType)
        }.insertSpecialCast(expression, valueType, expectedType)
    }

    return with(c.adapterGenerator) {
        expressionWithCast.applySuspendConversionIfNeeded(expression, substitutedExpectedType)
            .applySamConversionIfNeeded(expression)
    }
}


private inline fun IrVararg.applyConversionOnVararg(
    argument: FirExpression,
    crossinline conversion: IrExpression.(FirExpression) -> IrExpression,
): IrVararg {
    if (argument !is FirVarargArgumentsExpression || argument.arguments.size != elements.size) {
        return this
    }
    val argumentMapping = elements.zip(argument.arguments).toMap()
    // [IrTransformer] is not preferred, since it's hard to visit vararg elements only.
    elements.replaceAll { irVarargElement ->
        if (irVarargElement is IrExpression) {
            val firVarargArgument =
                argumentMapping[irVarargElement] ?: error("Can't find the original FirExpression for ${irVarargElement.render()}")
            irVarargElement.conversion(firVarargArgument)
        } else
            irVarargElement
    }
    return this
}

context(c: Fir2IrComponents)
fun IrStatementContainer.coerceStatementsToUnit(coerceLastExpressionToUnit: Boolean) {
    with(c.implicitCastInserter) {
        coerceStatementsToUnit(coerceLastExpressionToUnit)
    }
}

/**
 * Coerces single expressions and [IrContainerExpression]s (like [IrBlock]) to `Unit`.
 *
 * Non-[IrContainerExpression]s and [IrContainerExpression]s with a non-null [IrContainerExpression.origin], are wrapped in a coercion to unit type op if necessary.
 *
 * Blocks with a null [IrContainerExpression.origin], get coercion to unit inserted for the last expression.
 *
 * Note that non-last statements of [IrContainerExpression]s are not handled. It is expected that they are coerced to Unit somewhere else.
 */
context(c: Fir2IrComponents)
fun IrExpression.coerceToUnitHandlingSpecialBlocks(): IrExpression {
    return if (this is IrContainerExpression && origin == null) {
        val lastStatement = statements.lastOrNull()
        if (lastStatement is IrExpression) {
            statements[statements.lastIndex] = Fir2IrImplicitCastInserter.coerceToUnitIfNeeded(lastStatement)
        }
        this
    } else {
        Fir2IrImplicitCastInserter.coerceToUnitIfNeeded(this)
    }
}