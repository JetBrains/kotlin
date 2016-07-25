// IS_APPLICABLE: false

open class Bar(val a: Int = 0)

class Foo : Bar {
    <caret>lateinit var bar: String

    constructor() : super() {
        bar = ""
    }

    constructor(a: Int) : super(a) {
    }
}