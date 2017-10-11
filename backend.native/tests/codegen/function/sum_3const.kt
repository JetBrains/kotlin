package codegen.function.sum_3const

import kotlin.test.*

fun sum3():Int = sum(1, 2, 33)
fun sum(a:Int, b:Int, c:Int): Int = a + b + c

@Test fun runTest() {
    if (sum3() != 36) throw Error()
}