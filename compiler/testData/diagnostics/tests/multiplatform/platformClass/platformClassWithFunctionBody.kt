// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
header class Foo(
        <!HEADER_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val constructorProperty: String<!>,
        constructorParameter: String
) {
    <!HEADER_DECLARATION_WITH_BODY!>init<!> {
        <!UNUSED_EXPRESSION, UNUSED_EXPRESSION!>"no"<!>
    }

    <!HEADER_DECLARATION_WITH_BODY!>constructor(s: String)<!> {
        <!UNUSED_EXPRESSION!>"no"<!>
    }

    constructor() : <!HEADER_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("no")

    val prop: String = <!HEADER_PROPERTY_INITIALIZER!>"no"<!>

    var getSet: String
        <!HEADER_DECLARATION_WITH_BODY!>get()<!> = "no"
        <!HEADER_DECLARATION_WITH_BODY!>set(value)<!> {}

    fun defaultArg(<!HEADER_DECLARATION_WITH_DEFAULT_PARAMETER!>value: String = "no"<!>)

    <!HEADER_DECLARATION_WITH_BODY!>fun functionWithBody(x: Int): Int<!> {
        return x + 1
    }
}
