// KT-15642
open class Base {
    init { f() }
    open fun f() { }
}

class Derived : Base() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val s = "Hello"<!>
    override fun f() { s.hashCode() }
}

class B : A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b = "Hello"<!>
    override fun foo() {
        b.hashCode()
    }
}

// KT-13442
open class A {
    constructor() {
        runLater(this::foo)
    }

    open fun foo() {
    }

    private fun runLater(f: () -> Unit) {
    }
}


