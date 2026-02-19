// WITH_STDLIB

import kotlin.test.*

fun sum(a:Float, b:Int) = a + b

fun box(): String {
    assertEquals(3.0F, sum(1.0F, 2))
    return "OK"
}
