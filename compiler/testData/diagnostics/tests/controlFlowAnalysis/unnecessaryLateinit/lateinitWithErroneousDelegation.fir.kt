// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    lateinit var x: String

    constructor() {
        x = "Foo"
    }

    constructor(x: String, y: String): <!INAPPLICABLE_CANDIDATE!>this<!>(y.hashCode())
}