// LAMBDAS: CLASS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// Should be unmuted for JS when KT-79471 is fixed
// NO_CHECK_LAMBDA_INLINING

// FILE: lib.kt
package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

inline fun <reified R> typeOf2(e: R): KType = typeOf<R>()

fun <K> g(k: K): KType =
    run2 {
        typeOf2(listOf(k))
    }

fun run2(f: () -> KType): KType = f()

// FILE: main.kt
package test

import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun box(): String {
    val s = g("").toString()
    if (s != "kotlin.collections.List<K>") return "Fail: $s"

    return "OK"
}
