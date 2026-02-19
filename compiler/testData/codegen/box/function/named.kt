// WITH_STDLIB

import kotlin.test.*

fun foo(a:Int, b:Int) = a - b

fun box(): String {
    assertEquals(18, foo(b = 24, a = 42))
    return "OK"
}
