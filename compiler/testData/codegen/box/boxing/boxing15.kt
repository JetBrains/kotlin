// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    val res1 = foo(17)
    if (res1 != 17) return "FAIL 1: $res1"

    val nonConst = 17
    val res2 = foo(nonConst)
    if (res2 != 17) return "FAIL 2: $res2"

    return "OK"
}

fun <T : Int> foo(x: T): Int = x
