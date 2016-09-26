// !DIAGNOSTICS: -UNUSED_PARAMETER

class Foo {
    <!UNNECESSARY_LATEINIT!>lateinit<!> var x: String

    constructor() {
        x = "Foo"
    }

    constructor(x: String, y: String): <!NONE_APPLICABLE!>this<!>(y.hashCode())
}