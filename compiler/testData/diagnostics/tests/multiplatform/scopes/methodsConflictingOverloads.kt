// LANGUAGE: +MultiPlatformProjects
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect class <!NO_ACTUAL_FOR_EXPECT!>A<!> {
    fun foo()
}

expect abstract class <!NO_ACTUAL_FOR_EXPECT!>B<!>

expect class <!NO_ACTUAL_FOR_EXPECT!>C<!> : B

expect abstract class <!NO_ACTUAL_FOR_EXPECT!>D<!>() {
    fun <!AMBIGUOUS_ACTUALS{JVM}!>foo<!>()
}

class <!CONFLICTING_INHERITED_JVM_DECLARATIONS{JVM}!>E<!> : D()

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    fun foo()
}

actual class A : I {
    actual fun <!VIRTUAL_MEMBER_HIDDEN!>foo<!>() {}
}

actual abstract class B() {
    fun foo() {}
}

actual class C : B(), I {}

actual abstract class D {
    <!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
    <!CONFLICTING_OVERLOADS!>fun <!ACTUAL_MISSING!>foo<!>()<!> {}
}
