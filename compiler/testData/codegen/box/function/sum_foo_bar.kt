// WITH_STDLIB

import kotlin.test.*

fun foo(a:Int):Int = a
fun bar(a:Int):Int = a

fun sumFooBar(a:Int, b:Int):Int = foo(a) + bar(b)

fun box(): String {
    assertEquals(5, sumFooBar(2, 3))
    return "OK"
}
