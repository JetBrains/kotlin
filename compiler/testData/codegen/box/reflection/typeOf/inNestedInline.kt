// WITH_REFLECT
// TARGET_BACKEND: JVM
// TARGET_BACKEND: NATIVE, JS_IR

// MODULE: lib
package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

inline fun <reified T2, T3> typeOfPair() = typeOf<Pair<T2, T3>>()

// MODULE: main(lib)
import test.*

fun box() : String {
    val result3 = typeOfPair<String, Int>().toString()
    if (result3 != "test.Pair<kotlin.String, T3>") return "FAIL 3: $result3"
    return "OK"
}