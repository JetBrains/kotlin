interface A {
    val <caret>foo: String
}

class B: A {
    override val foo: String
        get() {
            return "B"
        }
}