// ISSUE: KT-90705
// LANGUAGE: +CompanionBlocks

import kotlin.test.assertEquals

open class A {
    companion {
        fun foo(x: Int = 0) = "A$x"
    }
}

class B : A() {
    companion {
        fun foo(x: Int = 1) = "B$x"
    }
}

fun box(): String {
    assertEquals("A0", A.foo())
    assertEquals("A42", A.foo(42))
    assertEquals("B1", B.foo())
    assertEquals("B42", B.foo(42))
    return "OK"
}
