package codegen.function.sum_foo_bar

import kotlin.test.*

fun foo(a:Int):Int = a
fun bar(a:Int):Int = a

fun sumFooBar(a:Int, b:Int):Int = foo(a) + bar(b)

@Test fun runTest() {
    if (sumFooBar(2, 3) != 5) throw Error()
}