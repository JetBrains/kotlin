// WITH_STDLIB

import kotlin.test.*

fun sum3():Int = sum(1, 2, 33)
fun sum(a:Int, b:Int, c:Int): Int = a + b + c

fun box(): String {
    assertEquals(36, sum3())
    return "OK"
}
