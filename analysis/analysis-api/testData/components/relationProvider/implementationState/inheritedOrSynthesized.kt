open class Base {
    open fun foo() {}
    open val bar: Int
        get() = 0
}

class <caret>Child : Base()
