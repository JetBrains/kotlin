// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A {
    fun foo()
}

expect abstract class B

expect class C : B

expect abstract class D() {
    fun foo()
}

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

actual abstract class D {
    <!CONFLICTING_OVERLOADS!>actual fun foo()<!> {}
    <!CONFLICTING_OVERLOADS!>fun <!ACTUAL_MISSING!>foo<!>()<!> {}
}
