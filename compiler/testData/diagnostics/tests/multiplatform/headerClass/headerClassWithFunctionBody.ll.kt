// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    <!EXPECTED_DECLARATION_WITH_BODY!>init<!> {
        "no"
    }

    <!EXPECTED_DECLARATION_WITH_BODY!>constructor(s: String)<!> {
        "no"
    }

    constructor() : <!EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("no")

    val prop: String = <!EXPECTED_PROPERTY_INITIALIZER!>"no"<!>

    var getSet: String
        <!EXPECTED_DECLARATION_WITH_BODY!>get()<!> = "no"
        <!EXPECTED_DECLARATION_WITH_BODY!>set(value)<!> {}

    <!EXPECTED_DECLARATION_WITH_BODY!>fun functionWithBody(x: Int): Int<!> {
        return x + 1
    }
}

// MODULE: m1-jvm()()(m1-common)
