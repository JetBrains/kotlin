/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.builtins

import org.jetbrains.kotlin.name.FqName

val compileTimeAnnotation = FqName("kotlin.CompileTimeCalculation")

data class CompileTimeFunction(val methodName: String, val args: List<String>)

@Suppress("UNCHECKED_CAST")
fun <T> unaryOperation(
    methodName: String, receiverType: String, function: (T) -> Any
): Pair<CompileTimeFunction, Function1<Any?, Any>> {
    return CompileTimeFunction(methodName, listOf(receiverType)) to function as Function1<Any?, Any>
}

@Suppress("UNCHECKED_CAST")
fun <T, E> binaryOperation(
    methodName: String, receiverType: String, parameterType: String, function: (T, E) -> Any
): Pair<CompileTimeFunction, Function2<Any?, Any?, Any>> {
    return CompileTimeFunction(methodName, listOfNotNull(receiverType, parameterType)) to function as Function2<Any?, Any?, Any>
}
