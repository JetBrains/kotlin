// LAMBDAS: CLASS
// WITH_REFLECT
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// Should be unmuted for JS when KT-79471 is fixed

// FILE: lib.kt
package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified R> typeOf2(e: R): KType = typeOf<R>()

// FILE: main.kt
package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun <K> g(k: K): KType =
    run {
        typeOf2(listOf(k))
    }

fun box(): String {
    val s = g("").toString()
    if (s != "kotlin.collections.List<K>") return "Fail: $s"

    return "OK"
}
