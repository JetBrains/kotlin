abstract class A {
    val y = foo().hashCode()

    abstract fun foo(): String
}

class ErrorImpl : A() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val x = "Hello"<!>
    override fun foo(): String = x
}

class CorrectImpl : A() {
    val x = "Hello"
    override fun foo(): String = "World"
}

// KT-43019
open class C(open val v: String) {
    val present = v != null
}

class D(<!ACCESS_TO_UNINITIALIZED_VALUE!>override val v: String<!>) : A(v)

// KT-15642
open class Base {
    init { f() }
    open fun f() { }
}

class Derived : Base() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val s = "Hello"<!>
    override fun f() { s.hashCode() }
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

class B : A {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b = "Hello"<!>
    override fun foo() {
        b.hashCode()
    }
}
