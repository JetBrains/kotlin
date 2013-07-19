trait A {
    val <caret>foo: String
}

trait Z {
    val foo: String
}

class B: A, Z {
    override val foo: String
        get() {
            return "B"
        }
}