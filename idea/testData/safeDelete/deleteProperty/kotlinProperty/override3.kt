open class A {
    open var foo: String
        get() {
            return "A"
        }
        set(value: String) {
            println()
        }
}

class B: A() {
    override var <caret>foo: String
        get() {
            return "B"
        }
        set(value: String) {
            println()
        }
}