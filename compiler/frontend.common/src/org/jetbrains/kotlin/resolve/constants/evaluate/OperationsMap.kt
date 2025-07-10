/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.constants.evaluate

import org.jetbrains.kotlin.builtins.EvalMethodNotFoundException
import org.jetbrains.kotlin.builtins.evalBinaryFunction
import org.jetbrains.kotlin.builtins.evalUnaryFunction
import org.jetbrains.kotlin.builtins.legacyK1EvalBinaryBigIntFunction
import java.math.BigInteger


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

        // This is only needed for K1, in K2 the types are already correct
        name =="plus" && leftType == CompileTimeType.STRING -> "kotlin.Any?"
        else -> rightType.toKotlinTypeName()
    }

    try {
        return evalBinaryFunction(name, leftType.toKotlinTypeName(), rewrittenRightType, left, right)
    } catch (e: EvalMethodNotFoundException) {
        return null;
    }
}

fun checkBinaryOp(
    name: String, leftType: CompileTimeType, left: BigInteger, rightType: CompileTimeType, right: BigInteger,
): BigInteger? {
    try {
        return legacyK1EvalBinaryBigIntFunction(name, leftType.toKotlinTypeName(), rightType.toKotlinTypeName(), left, right)
    } catch (e: EvalMethodNotFoundException) {
        return null;
    }
}
