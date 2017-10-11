package runtime.basic.tostring1

import kotlin.test.*

@Test fun runTest() {
    val hello = "Hello world"
    println(hello.subSequence(1, 5).toString())
}