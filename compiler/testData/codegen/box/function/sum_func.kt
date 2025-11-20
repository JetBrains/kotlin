// WITH_STDLIB

import kotlin.test.*

fun foo():Int = 1
fun bar():Int = 2
fun sum():Int = foo() + bar()

fun box(): String {
    assertEquals(3, sum())
    return "OK"
}
