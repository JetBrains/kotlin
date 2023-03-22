// IGNORE_REVERSED_RESOLVE
// MODULE: m1-common
// FILE: common.kt
expect class Foo(
        val constructorProperty: String,
        constructorParameter: String
) {
    init {
        "no"
    }

    constructor(s: String) {
        "no"
    }

    constructor() : this("no")

    val prop: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

    var getSet: String
        get() = "no"
        set(value) {}

    <!EXPECTED_DECLARATION_WITH_BODY!>fun functionWithBody(x: Int): Int<!> {
        return x + 1
    }
}
