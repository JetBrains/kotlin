// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM
// TARGET_BACKEND: NATIVE, JS_IR

// MODULE: lib
// FILE: lib.kt
package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))
inline fun <T4, T5> typeOfPair2(x: T4, y: T5) = typeOfPair(Pair(x, y), y)

inline fun <reified T> typeOfWrapped() = typeOf<T>()
inline fun <reified T2, T3> typeOfPair3() = typeOfWrapped<Pair<T2, T3>>()

// MODULE: main(lib)
// FILE: main.kt
import test.*

fun box() : String {
    //if (typeOfPair2_local("1", 1) != "OK") return "FAIL 1"
    //if (typeOfPair2("1", 1).toString() != "test.Pair<test.Pair<T4, T5>, T3>") return "FAIL 3: ${typeOfPair2("1", 1)}"
    if (typeOfPair3<String, Int>().toString() != "test.Pair<kotlin.String, T3>") return "FAIL 4: ${typeOfPair3<String, Int>()}"
    return "OK"
}

//inline fun <T4, T5> typeOfPair2_local(x: T4, y: T5) = "OK"
