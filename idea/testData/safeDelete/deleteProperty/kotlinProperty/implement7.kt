interface A {
    val <caret>foo: String
}

interface Z {
    val foo: String
}

class B: A, Z {
    override val foo: String
        get() {
            return "B"
        }
}