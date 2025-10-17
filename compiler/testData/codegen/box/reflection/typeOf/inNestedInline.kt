// DUMP_IR_OF_PREPROCESSED_INLINE_FUNCTIONS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME
// WASM_ALLOW_FQNAME_IN_KCLASS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// Should be unmuted for JS when KT-79471 is fixed

// FILE: lib.kt
package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))
inline fun <T4, T5> typeOfPair2(x: T4, y: T5) = typeOfPair(Pair(x, y), y)

// FILE: main.kt
package test;

fun box() : String {
    if (typeOfX("1").toString() != "kotlin.String") return "FAIL 1: ${typeOfX("1")}"
    if (typeOfPair("1", 1).toString() != "test.Pair<kotlin.String, T3>") return "FAIL 2: ${typeOfPair("1", 1)}"
    if (typeOfPair2("1", 1).toString() != "test.Pair<test.Pair<T4, T5>, T3>") return "FAIL 3: ${typeOfPair2("1", 1)}"
    return "OK"
}