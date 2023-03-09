// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

open class Base {
    open fun foo() {}

    open val bar: String = ""

    override fun hashCode() = super.hashCode()
}

class Derived : Base() {
    override fun foo() {
        super.foo()
    }

    override val bar: String
        get() = super.bar
}
