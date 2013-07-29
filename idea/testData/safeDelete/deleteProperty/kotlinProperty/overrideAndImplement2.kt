open class A {
    open val <caret>foo: String
        get() {
            return "A"
        }
}

trait Z {
    val foo: String
}

class B: A(), Z {
    override val foo: String
        get() {
            return "B"
        }
}