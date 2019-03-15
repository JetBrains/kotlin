// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

class Foo {
    <!JVM:NON_ABSTRACT_FUNCTION_WITH_NO_BODY, NON_ABSTRACT_FUNCTION_WITH_NO_BODY!><!JVM:WRONG_MODIFIER_TARGET, WRONG_MODIFIER_TARGET!>expect<!> fun bar(): String<!>
}

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt
