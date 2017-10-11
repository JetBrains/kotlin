package codegen.inline.inline20

import kotlin.test.*

@Suppress("NOTHING_TO_INLINE")
inline fun bar(block: () -> String) : String {
    return block()
}

fun bar2() : String {
    return bar { return "def" }
}

@Test fun runTest() {
    println(bar2())
}
