// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    lateinit var bar: String

    constructor() {
        bar = ""
    }

    constructor(a: Int) : this(a, 0, 0) {
    }

    constructor(a: Int, b: Int) : this(a) {
    }

    constructor(a: Int, b: Int, c: Int) : this(a, b) {
    }
}
