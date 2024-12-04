// WITH_REFLECT
// TARGET_BACKEND: JVM
// TARGET_BACKEND: NATIVE

package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))
inline fun <T4, T5> typeOfPair2(x: T4, y: T5) = typeOfPair(Pair(x, y), y)

fun box() : String {
    if (typeOfX("1").toString() != "kotlin.String") return "FAIL 1: ${typeOfX("1")}"
    if (typeOfPair("1", 1).toString() != "test.Pair<kotlin.String, T3>") return "FAIL 2: ${typeOfPair("1", 1)}"
    if (typeOfPair2("1", 1).toString() != "test.Pair<test.Pair<T4, T5>, T3>") return "FAIL 3: ${typeOfPair2("1", 1)}"
    return "OK"
}