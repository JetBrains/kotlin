// FILE: A.kt
package test;

import kotlin.reflect.*

class Pair<A, B>(val x: A, val y: B)

private inline fun <reified T1> typeOfX(x: T1) = typeOf<T1>()
private inline fun <reified T2, T3> typeOfPair(x: T2, y: T3) = typeOfX(Pair(x, y))
internal inline fun <T4, T5> typeOfPair2(x: T4, y: T5) = typeOfPair(Pair(x, y), y)

// FILE: B.kt
import test.*

fun box() : String {
    if (typeOfPair2("1", 1).toString() != "test.Pair<test.Pair<T4, T5>, T3>") return "FAIL 3: ${typeOfPair2("1", 1)}"
    return "OK"
}

// FILE: A.kt
class A

// FILE: B.kt
private fun A.privateExtension() = "OK"
private inline fun A.privateInlineExtension1() = privateExtension()
internal inline fun A.internalInlineExtension() = privateInlineExtension1() + "1"

// FILE: main.kt
internal fun topLevelFun() = A().internalInlineExtension() + "2"

fun box(): String {
    val result = topLevelFun()
    if (result != "OK12") return result
    return "OK"
}
