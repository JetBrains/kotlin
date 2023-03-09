// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

open class Base {
    open fun foo() {}

    open val bar: String = ""
}

interface BaseI {
    fun foo()
    val bar: String
}

class Derived : Base(), BaseI {
    override fun foo() {
        super.foo()
    }

    override val bar: String
        get() = super.bar
}
