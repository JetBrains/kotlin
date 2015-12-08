open class A {
    open val foo: String
        get() {
            return "A"
        }
}

class B: A() {
    override val <caret>foo: String
        get() {
            return "B"
        }
}