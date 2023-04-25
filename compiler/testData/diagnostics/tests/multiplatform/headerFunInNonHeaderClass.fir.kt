// MODULE: m1-common
// FILE: common.kt

class Foo {
    <!NON_ABSTRACT_FUNCTION_WITH_NO_BODY, NO_ACTUAL_FOR_EXPECT{JVM}!><!WRONG_MODIFIER_TARGET!>expect<!> fun bar(): String<!>
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
