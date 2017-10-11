package codegen.function.sum_imm

import kotlin.test.*

fun sum(a:Int): Int = a + 33

@Test fun runTest() {
    if (sum(2) != 35) throw Error()
}