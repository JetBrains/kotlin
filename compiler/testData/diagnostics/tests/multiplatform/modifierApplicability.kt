// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!WRONG_MODIFIER_TARGET!>platform<!> typealias Foo = String

class Outer <!WRONG_MODIFIER_TARGET!>platform<!> constructor() {
    platform class Nested

    <!WRONG_MODIFIER_TARGET!>platform<!> init {}

    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!><!WRONG_MODIFIER_TARGET!>platform<!> fun foo()<!>
    <!WRONG_MODIFIER_TARGET!>platform<!> val bar: Int
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>platform<!> fun localFun()
    <!WRONG_MODIFIER_TARGET!>platform<!> var <!UNUSED_VARIABLE!>x<!> = 42
    <!WRONG_MODIFIER_TARGET!>platform<!> class Bar
}

// MODULE: m2-jvm
// FILE: jvm.kt

class Outer impl constructor() {
    <!PLATFORM_DEFINITION_WITHOUT_DECLARATION!>impl<!> class Nested

    <!WRONG_MODIFIER_TARGET!>impl<!> init {}
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>impl<!> fun localFun() {}
    <!WRONG_MODIFIER_TARGET!>impl<!> var <!UNUSED_VARIABLE!>x<!> = 42
    <!PLATFORM_DEFINITION_WITHOUT_DECLARATION, WRONG_MODIFIER_TARGET!>impl<!> class Bar
}
