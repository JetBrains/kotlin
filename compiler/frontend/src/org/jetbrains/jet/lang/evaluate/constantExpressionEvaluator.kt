package org.jetbrains.jet.lang.evaluate

import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.resolve.name.Name

fun evaluateBinaryExpression(firstCompileTimeConstant: CompileTimeConstant<*>, secondCompileTimeConstant: CompileTimeConstant<*>, functionName: Name): Any? {
    val compileTimeTypeFirst = getCompileTimeType(firstCompileTimeConstant)
    val compileTimeTypeSecond = getCompileTimeType(secondCompileTimeConstant)
    if (compileTimeTypeFirst == null || compileTimeTypeSecond == null) {
        return null
    }

    val first = firstCompileTimeConstant.getValue()
    val second = secondCompileTimeConstant.getValue()

    val function = binaryOperations[BinaryOperation(compileTimeTypeFirst, compileTimeTypeSecond, functionName)]
    if (function != null)  {
        return function(first, second)
    }

    return null
}

fun getCompileTimeType(c: CompileTimeConstant<*>): CompileTimeType<out Any>? = when (c.getValue()) {
    is Int -> INT
    is Byte -> BYTE
    is Short -> SHORT
    is Long -> LONG
    is Double -> DOUBLE
    is Float -> FLOAT
    is Char -> CHAR
    is Boolean -> BOOLEAN
    is String -> STRING
    else -> null
}

private class CompileTimeType<T>

private val BYTE = CompileTimeType<Byte>()
private val SHORT = CompileTimeType<Short>()
private val INT = CompileTimeType<Int>()
private val LONG = CompileTimeType<Long>()
private val DOUBLE = CompileTimeType<Double>()
private val FLOAT = CompileTimeType<Float>()
private val CHAR = CompileTimeType<Char>()
private val BOOLEAN = CompileTimeType<Boolean>()
private val STRING = CompileTimeType<String>()

private fun <A, B> bOp(a: CompileTimeType<A>, b: CompileTimeType<B>, functionNameAsString: String, f: (A, B) -> Any) = BinaryOperation(a, b, Name.identifier(functionNameAsString)) to f  as Function2<Any?, Any?, Any>

private data class BinaryOperation<A, B>(val f: CompileTimeType<out A>, val s: CompileTimeType<out B>, val functionName: Name)

private val binaryOperations = hashMapOf<BinaryOperation<*, *>, (Any?, Any?) -> Any>(
        // String
        bOp(STRING, STRING, "plus", { a, b -> a + b }),
        bOp(STRING, BYTE,   "plus", { a, b -> a + b }),
        bOp(STRING, SHORT,  "plus", { a, b -> a + b }),
        bOp(STRING, INT,    "plus", { a, b -> a + b }),
        bOp(STRING, LONG,   "plus", { a, b -> a + b }),
        bOp(STRING, DOUBLE, "plus", { a, b -> a + b }),
        bOp(STRING, FLOAT,  "plus", { a, b -> a + b }),
        bOp(STRING, CHAR,   "plus", { a, b -> a + b })
)

