// LANGUAGE: +CompanionBlocks

import kotlin.test.assertEquals

open class A {
    companion {
        fun foo() = "static A::foo"
    }
}

open class B : A() {
    fun foo() = "instance B::foo"
}

class C : B()

fun box(): String {
    assertEquals("static A::foo", A.foo())
    assertEquals("instance B::foo", B().foo())
    assertEquals("instance B::foo", C().foo())
    return "OK"
}
