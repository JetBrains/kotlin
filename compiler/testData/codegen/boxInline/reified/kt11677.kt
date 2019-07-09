// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// FULL_JDK
// WITH_REFLECT

package test

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

open class TypeLiteral<T> {
    val type: Type
        get() = (javaClass.genericSuperclass as ParameterizedType).getActualTypeArguments()[0]
}

// normal inline function works fine
inline fun <reified T> typeLiteral(): TypeLiteral<T> = object : TypeLiteral<T>() {}

// nested lambda loses reification of T
inline fun <reified T> brokenTypeLiteral(): TypeLiteral<T> = "".run { typeLiteral<T>() }

// FILE: 2.kt

import test.*

fun box(): String {
    val type1 = typeLiteral<List<String>>().type.toString()
    if (type1 != "java.util.List<? extends java.lang.String>") return "fail 1: $type1"

    val type2 = brokenTypeLiteral<List<String>>().type.toString()
    if (type2 != "java.util.List<? extends java.lang.String>") return "fail 2: $type2"

    return "OK"
}
