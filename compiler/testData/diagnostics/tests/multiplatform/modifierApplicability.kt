// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!WRONG_MODIFIER_TARGET!>header<!> typealias Foo = String

class Outer <!WRONG_MODIFIER_TARGET!>header<!> constructor() {
    header class Nested

    <!WRONG_MODIFIER_TARGET!>header<!> init {}

    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!><!WRONG_MODIFIER_TARGET!>header<!> fun foo()<!>
    <!WRONG_MODIFIER_TARGET!>header<!> val bar: Int
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>header<!> fun localFun()
    <!WRONG_MODIFIER_TARGET!>header<!> var <!UNUSED_VARIABLE!>x<!> = 42
    <!WRONG_MODIFIER_TARGET!>header<!> class Bar
}

// MODULE: m2-jvm
// FILE: jvm.kt

class Outer impl constructor() {
    impl class <!IMPLEMENTATION_WITHOUT_HEADER!>Nested<!>

    <!WRONG_MODIFIER_TARGET!>impl<!> init {}
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>impl<!> fun localFun() {}
    <!WRONG_MODIFIER_TARGET!>impl<!> var <!UNUSED_VARIABLE!>x<!> = 42
    <!WRONG_MODIFIER_TARGET!>impl<!> class <!IMPLEMENTATION_WITHOUT_HEADER!>Bar<!>
}
