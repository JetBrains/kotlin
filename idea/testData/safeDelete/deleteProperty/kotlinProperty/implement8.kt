trait A {
    var <caret>foo: String
}

trait Z {
    var foo: String
}

class B: A, Z {
    override val foo: String
        get() {
            return "B"
        }
        set(value: String) {

        }
}