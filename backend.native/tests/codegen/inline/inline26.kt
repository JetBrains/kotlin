package codegen.inline.inline26

import kotlin.test.*

inline fun call(block1: () -> Unit, noinline block2: () -> Int): Int {
    block1()
    return block2()
}

@Test fun runTest() {
    var x = 5
    println(call({ x = 7 }, x::toInt))
}
