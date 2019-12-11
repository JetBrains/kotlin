// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : this(a) {
        bar = "a"
    }
}
