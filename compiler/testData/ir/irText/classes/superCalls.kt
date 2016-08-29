open class Base {
    open fun foo() {}

    open val bar: String = ""
}

class Derived : Base() {
    override fun foo() {
        super.foo()
    }

    override val bar: String
        get() = super.bar
}