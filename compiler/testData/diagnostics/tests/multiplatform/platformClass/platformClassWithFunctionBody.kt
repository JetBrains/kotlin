// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
platform class Foo(
        <!PLATFORM_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER!>val constructorProperty: String<!>,
        constructorParameter: String
) {
    <!PLATFORM_DECLARATION_WITH_BODY!>init<!> {
        <!UNUSED_EXPRESSION, UNUSED_EXPRESSION!>"no"<!>
    }

    <!PLATFORM_DECLARATION_WITH_BODY!>constructor(s: String)<!> {
        <!UNUSED_EXPRESSION!>"no"<!>
    }

    constructor() : <!PLATFORM_CLASS_CONSTRUCTOR_DELEGATION_CALL!>this<!>("no")

    val prop: String = <!PLATFORM_PROPERTY_INITIALIZER!>"no"<!>

    var getSet: String
        <!PLATFORM_DECLARATION_WITH_BODY!>get()<!> = "no"
        <!PLATFORM_DECLARATION_WITH_BODY!>set(value)<!> {}

    fun defaultArg(<!PLATFORM_DECLARATION_WITH_DEFAULT_PARAMETER!>value: String = "no"<!>)

    <!PLATFORM_DECLARATION_WITH_BODY!>fun functionWithBody(x: Int): Int<!> {
        return x + 1
    }
}
