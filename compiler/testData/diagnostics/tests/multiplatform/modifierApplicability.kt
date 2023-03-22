// IGNORE_REVERSED_RESOLVE
// MODULE: m1-common
// FILE: common.kt

<!WRONG_MODIFIER_TARGET!>expect<!> typealias Foo = String

class Outer <!WRONG_MODIFIER_TARGET!>expect<!> constructor() {
    <!WRONG_MODIFIER_TARGET!>expect<!> class Nested

    <!WRONG_MODIFIER_TARGET!>expect<!> init {}

    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!><!WRONG_MODIFIER_TARGET!>expect<!> fun foo()<!>
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!><!WRONG_MODIFIER_TARGET!>expect<!> val bar: Int<!>
}

fun foo() {
    <!NON_MEMBER_FUNCTION_NO_BODY!><!WRONG_MODIFIER_TARGET!>expect<!> fun localFun()<!>
    <!WRONG_MODIFIER_TARGET!>expect<!> var x = 42
    <!WRONG_MODIFIER_TARGET!>expect<!> class Bar
}

// MODULE: m2-jvm
// FILE: jvm.kt

class Outer <!ACTUAL_WITHOUT_EXPECT!>actual constructor()<!> {
    actual class <!ACTUAL_WITHOUT_EXPECT!>Nested<!>

    <!WRONG_MODIFIER_TARGET!>actual<!> init {}
}

fun foo() {
    <!WRONG_MODIFIER_TARGET!>actual<!> fun localFun() {}
    <!WRONG_MODIFIER_TARGET!>actual<!> var x = 42
    <!WRONG_MODIFIER_TARGET!>actual<!> class <!ACTUAL_WITHOUT_EXPECT!>Bar<!>
}
