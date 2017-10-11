package codegen.function.defaults4

import kotlin.test.*

open class A {
    open fun foo(x: Int = 42) = println(x)
}

open class B : A()

class C : B() {
    override fun foo(x: Int) = println(x + 1)
}

@Test fun runTest() {
    C().foo()
}