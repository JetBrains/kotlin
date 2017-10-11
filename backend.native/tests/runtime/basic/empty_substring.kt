package runtime.basic.empty_substring

import kotlin.test.*

@Test fun runTest() {
    val hello = "Hello world"
    println(hello.subSequence(1, 1).toString())
}