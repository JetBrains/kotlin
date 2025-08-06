/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.builtins

import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterMethodNotFoundError
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy

fun interpretUnaryFunction(name: String, type: String, a: Any?): Any? {
    when (name) {
        "dec" -> when (type) {
            "kotlin.Char" -> return (a as Char).dec()
            "kotlin.Byte" -> return (a as Byte).dec()
            "kotlin.Short" -> return (a as Short).dec()
            "kotlin.Int" -> return (a as Int).dec()
            "kotlin.Float" -> return (a as Float).dec()
            "kotlin.Long" -> return (a as Long).dec()
            "kotlin.Double" -> return (a as Double).dec()
        }
        "inc" -> when (type) {
            "kotlin.Char" -> return (a as Char).inc()
            "kotlin.Byte" -> return (a as Byte).inc()
            "kotlin.Short" -> return (a as Short).inc()
            "kotlin.Int" -> return (a as Int).inc()
            "kotlin.Float" -> return (a as Float).inc()
            "kotlin.Long" -> return (a as Long).inc()
            "kotlin.Double" -> return (a as Double).inc()
        }
    }

    try {
        return evalUnaryFunction(name, type, a)
    } catch (e: EvalMethodNotFoundException) {
        throw InterpreterMethodNotFoundError("Unknown function: $name($type)")
    }
}

internal fun interpretBinaryFunction(name: String, typeA: String, typeB: String, a: Any?, b: Any?): Any? {
    if (name == "EQEQEQ") {
        when (typeA) {
            "kotlin.Any?" -> if (typeB == "kotlin.Any?") return if (a is Proxy && b is Proxy) a.state === b.state else a === b
        }
    }

    try {
        return evalBinaryFunction(name, typeA, typeB, a, b);
    } catch (e: EvalMethodNotFoundException) {
        throw InterpreterMethodNotFoundError("Unknown function: $name($typeA, $typeB)")
    }
}

internal fun interpretTernaryFunction(name: String, typeA: String, typeB: String, typeC: String, a: Any?, b: Any?, c: Any?): Any {
    try {
        return evalTernaryFunction(name, typeA, typeB, typeC, a, b, c);
    } catch (e: EvalMethodNotFoundException) {
        throw InterpreterMethodNotFoundError("Unknown function: $name($typeA, $typeB, $typeC)")
    }
}