open class A {
    open val <caret>foo: String
        get() {
            return "A"
        }
}

class B: A() {
    override val foo: String
        get() {
            return "B"
        }
}