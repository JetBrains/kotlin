package lib2

import lib1.A

class B : A {
    override fun bar() = -42

    val unlinkedFunctionUsage get() = foo()
}

class B1 : A {
    override fun bar() = -42

    val unlinkedFunctionUsage = foo() // Expected failure on class instance initialization.
}
