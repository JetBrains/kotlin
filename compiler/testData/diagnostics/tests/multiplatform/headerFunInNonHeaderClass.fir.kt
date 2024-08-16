// MODULE: m1-common
// FILE: common.kt

class Foo {
    <!WRONG_MODIFIER_TARGET!>expect<!> <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY!>fun bar(): String<!>
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
