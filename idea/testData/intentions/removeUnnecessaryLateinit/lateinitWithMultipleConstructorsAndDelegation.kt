// INTENTION_TEXT: Remove unnecessary lateinit

class Foo {
    <caret>lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : this() {
    }

    constructor(a: Int, b: Int) : this(a) {
    }
}