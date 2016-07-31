// IS_APPLICABLE: false

open class Bar

class Foo : Bar {
    <caret>lateinit var bar: String

    constructor() : super() {
        bar = ""
    }

    constructor(a: Int) : super() {
    }
}