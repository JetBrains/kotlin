/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildArrayOfCall
import org.jetbrains.kotlin.fir.expressions.builder.buildConstExpression

object ConstantValueFactory {
    fun createArrayValue(value: List<FirExpression>): FirArrayOfCall {
        return buildArrayOfCall {
            arguments += value
        }
    }

    fun createConstantValue(value: Any?): FirExpression? {
        return when (value) {
            is Byte -> buildConstExpression(null, FirConstKind.Byte, value)
            is Short -> buildConstExpression(null, FirConstKind.Short, value)
            is Int -> buildConstExpression(null, FirConstKind.Int, value)
            is Long -> buildConstExpression(null, FirConstKind.Long, value)
            is Char -> buildConstExpression(null, FirConstKind.Char, value)
            is Float -> buildConstExpression(null, FirConstKind.Float, value)
            is Double -> buildConstExpression(null, FirConstKind.Double, value)
            is Boolean -> buildConstExpression(null, FirConstKind.Boolean, value)
            is String -> buildConstExpression(null, FirConstKind.String, value)
            is ByteArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is ShortArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is IntArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is LongArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is CharArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is FloatArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is DoubleArray -> createArrayValue(value.map { createConstantValue(it)!! })
            is BooleanArray -> createArrayValue(value.map { createConstantValue(it)!! })
            null -> buildConstExpression(null, FirConstKind.Null, value)
            else -> null
        }
    }
}
