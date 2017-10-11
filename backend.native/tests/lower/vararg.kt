package lower.vararg

import kotlin.test.*

fun foo(vararg x: Any?) {}
fun bar() = foo()

@Test fun runTest() {
  bar()
}