interface A {
    val foo: String
}

interface Z {
    val foo: String
}

class B: A, Z {
    override val <caret>foo: String
        get() {
            return "B"
        }
}