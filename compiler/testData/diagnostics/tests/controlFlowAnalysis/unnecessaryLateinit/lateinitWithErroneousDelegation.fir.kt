// DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    lateinit var x: String

    constructor() {
        x = "Foo"
    }

    constructor(x: String, y: String): <!NONE_APPLICABLE!>this<!>(y.hashCode())
}
