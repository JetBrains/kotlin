interface A {
    var foo: String
}

class B: A {
    override var <caret>foo: String
        get() {
            return "B"
        }
        set(value: String) {
            println()
        }
}