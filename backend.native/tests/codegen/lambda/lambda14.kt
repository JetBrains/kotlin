// FILE: 1.kt

package codegen.lambda.lambda14

import kotlin.test.*

@Test fun runTest() {
    assertEquals(foo()(), "foo1")
    assertEquals(foo(0)(), "foo2")
}

fun foo() = { "foo1" }

// FILE: 2.kt

package codegen.lambda.lambda14

fun foo(ignored: Int) = { "foo2" }
