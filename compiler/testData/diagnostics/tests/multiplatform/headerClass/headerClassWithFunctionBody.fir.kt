// IGNORE_REVERSED_RESOLVE
// MODULE: m1-common
// FILE: common.kt
<!NO_ACTUAL_FOR_EXPECT!>expect class Foo(
        val constructorProperty: String,
        constructorParameter: String
) {
    <!EXPECTED_DECLARATION_WITH_BODY!>init<!> {
        "no"
    }

    <!EXPECTED_DECLARATION_WITH_BODY!>constructor(s: String)<!> {
        "no"
    }

    constructor() : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("no")

    val prop: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

    var getSet: String
        get() = "no"
        set(value) {}

    <!EXPECTED_DECLARATION_WITH_BODY!>fun functionWithBody(x: Int): Int<!> {
        return x + 1
    }
}<!>
