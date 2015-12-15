interface A {
    var <caret>foo: String
}

class B: A {
    override val foo: String
        get() {
            return "B"
        }
        set(value: String) {

        }
}