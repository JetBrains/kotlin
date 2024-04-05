// LANGUAGE: +MultiPlatformProjects
// DONT_STOP_ON_FIR_ERRORS

// MODULE: common
// FILE: common.kt
expect class A {
    fun foo()
}

expect abstract class B

expect class C : B

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect abstract class D() {
    <!AMBIGUOUS_ACTUALS{JVM}, NO_ACTUAL_FOR_EXPECT{JVM}!>fun foo()<!>
}<!>

class E : D()

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

actual abstract class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>D<!> {
    <!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
    <!CONFLICTING_OVERLOADS!>fun <!ACTUAL_MISSING!>foo<!>()<!> {}
}
