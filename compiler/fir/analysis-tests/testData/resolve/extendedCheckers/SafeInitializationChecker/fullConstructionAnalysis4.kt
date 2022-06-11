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

class B : A() {
    <!ACCESS_TO_UNINITIALIZED_VALUE!>val b = "Hello"<!>
    override fun foo() {
        b.hashCode()
    }
}
