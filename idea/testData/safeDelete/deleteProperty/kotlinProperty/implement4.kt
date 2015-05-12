interface A {
    var foo: String
}

interface Z {
    var foo: String
}

class B: A, Z {
    override var <caret>foo: String
        get() {
            return "B"
        }
        set(value: String) {
            println()
        }
}