trait A {
    val foo: String
}

trait Z {
    val foo: String
}

class B: A, Z {
    override val <caret>foo: String
        get() {
            return "B"
        }
}