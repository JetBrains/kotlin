// WITH_STDLIB

// MODULE: lib
// FILE: A.kt
package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

private inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
private inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))
@Suppress("NON_PUBLIC_INLINE_CALL_FROM_PUBLIC_INLINE")
public inline fun <T4, T5> typeOfPair2(x: T4, y: T5) = typeOfPair(Pair(x, y), y)

// MODULE: main(lib)
// FILE: B.kt
import test.*

fun box() : String {
    if (typeOfPair2("1", 1).toString().replace("test.", "") != "Pair<Pair<T4, T5>, T3>") return "FAIL 3: ${typeOfPair2("1", 1)}"
    return "OK"
}