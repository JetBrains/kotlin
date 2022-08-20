package lib2

import lib1.A

class B : A() {
    override fun baz() = -42

    val unlinkedFunctionUsage get() = foo() + bar()
}

class B1 : A() {
    override fun baz() = -42

    val unlinkedFunctionUsage = foo() // Expected failure on class instance initialization.
}

class B2 : A() {
    override fun baz() = -42

    val unlinkedFunctionUsage = bar() // Expected failure on class instance initialization.
}
