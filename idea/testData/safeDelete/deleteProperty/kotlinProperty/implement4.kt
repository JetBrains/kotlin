trait A {
    var foo: String
}

trait Z {
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