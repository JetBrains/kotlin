// INTENTION_TEXT: Remove unnecessary lateinit

class Foo {
    <caret>lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(baz: Int) {
        bar = ""
    }
}