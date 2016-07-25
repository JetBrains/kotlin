// IS_APPLICABLE: false

class Foo() {
    <caret>lateinit var bar: String

    constructor(baz: Int) : this() {
        bar = ""
    }
}