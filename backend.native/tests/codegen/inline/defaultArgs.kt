package codegen.inline.defaultArgs

import kotlin.test.*

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    println(y)
}

@Test fun runTest() {
    val z = Z()
    z.foo()
}