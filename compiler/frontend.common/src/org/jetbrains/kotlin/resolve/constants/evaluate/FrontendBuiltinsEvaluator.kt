/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants.evaluate

import org.jetbrains.kotlin.builtins.EvalMethodNotFoundException
import org.jetbrains.kotlin.builtins.evalBinaryFunction
import org.jetbrains.kotlin.builtins.evalUnaryFunction
import org.jetbrains.kotlin.builtins.legacyK1EvalBinaryBigIntFunction
import java.math.BigInteger
import java.util.Locale

fun CompileTimeType.toKotlinTypeName() = "kotlin.${toString().lowercase().replaceFirstChar {  it.titlecase(Locale.US) }}"

fun evalUnaryOp(name: String, type: CompileTimeType, value: Any): Any? {
    try {
        return evalUnaryFunction(name, type.toKotlinTypeName(), value)
    } catch (e: EvalMethodNotFoundException) {
        return null;
    }
}

fun evalBinaryOp(name: String, leftType: CompileTimeType, left: Any, rightType: CompileTimeType, right: Any): Any? {
    val rewrittenRightType = when  {
        name =="equals" -> "kotlin.Any?"
        name =="plus" && leftType == CompileTimeType.STRING -> "kotlin.Any?"
        else -> rightType.toKotlinTypeName()
    }

    try {
        return evalBinaryFunction(name, leftType.toKotlinTypeName(), rewrittenRightType, left, right)
    } catch (e: EvalMethodNotFoundException) {
        return null;
    }
}

// FIXME: This can be removed once K1 is dropped.
fun legacyK1EvalUnaryOp(name: String, type: CompileTimeType, a: Any): Any? {
    val allowedNames = listOf(
        "code",
        "inv",
        "length",
        "not",
        "toByte",
        "toChar",
        "toDouble",
        "toFloat",
        "toInt",
        "toLong",
        "toShort",
        "toString",
        "unaryMinus",
        "unaryPlus",
    )
    if (name !in allowedNames) return null
    return evalUnaryOp(name, type, a)
}

// FIXME: This can be removed once K1 is dropped.
fun legacyK1EvalBinaryOp(name: String, leftType: CompileTimeType, left: Any, rightType: CompileTimeType, right: Any): Any? {
    val allowedNames = listOf(
        "and",
        "compareTo",
        "div",
        "equals",
        "floorDiv",
        "get",
        "minus",
        "mod",
        "or",
        "plus",
        "rem",
        "shl",
        "shr",
        "times",
        "ushr",
        "xor",
    )
    if (name !in allowedNames) return null
    return evalBinaryOp(name, leftType, left, rightType, right)
}

// FIXME: This can be removed once K1 is dropped.
fun legacyK1CheckBinaryOp(
    name: String, leftType: CompileTimeType, left: BigInteger, rightType: CompileTimeType, right: BigInteger,
): BigInteger? {
    try {
        return null
        return legacyK1EvalBinaryBigIntFunction(name, leftType.toKotlinTypeName(), rightType.toKotlinTypeName(), left, right)
    } catch (e: EvalMethodNotFoundException) {
        return null;
    }
}