/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer.checker

import org.jetbrains.kotlin.backend.common.actualizer.IrExpectActualMatchingContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualCollectionArgumentsCompatibilityCheckStrategy

internal fun IrExpectActualMatchingContext.areIrExpressionConstValuesEqual(
    a: IrElement?,
    b: IrElement?,
    collectionArgumentsCompatibilityCheckStrategy: ExpectActualCollectionArgumentsCompatibilityCheckStrategy,
): Boolean {
    return when {
        a == null || b == null -> (a == null) == (b == null)

        a::class != b::class -> false

        a is IrConst<*> && b is IrConst<*> -> a.value == b.value

        a is IrClassReference && b is IrClassReference -> equalBy(a, b) {
            val classId = it.classType.classOrFail.classId
            getClassIdAfterActualization(classId)
        }

        a is IrGetEnumValue && b is IrGetEnumValue ->
            areCompatibleExpectActualTypes(a.type, b.type) && equalBy(a, b) { it.symbol.owner.name }

        a is IrVararg && b is IrVararg -> {
            collectionArgumentsCompatibilityCheckStrategy.areCompatible(a.elements, b.elements) { f, s ->
                areIrExpressionConstValuesEqual(f, s, collectionArgumentsCompatibilityCheckStrategy)
            }
        }

        a is IrConstructorCall && b is IrConstructorCall -> {
            equalBy(a, b) { it.valueArgumentsCount } &&
                    areCompatibleExpectActualTypes(a.type, b.type) &&
                    (0..<a.valueArgumentsCount).all { i ->
                        areIrExpressionConstValuesEqual(
                            a.getValueArgument(i),
                            b.getValueArgument(i),
                            collectionArgumentsCompatibilityCheckStrategy
                        )
                    }
        }

        else -> error("Not handled expression types $a $b")
    }
}

private inline fun <T : Any> equalBy(first: T, second: T, selector: (T) -> Any?): Boolean =
    selector(first) == selector(second)
