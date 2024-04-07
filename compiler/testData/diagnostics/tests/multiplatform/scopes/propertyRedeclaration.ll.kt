// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A {
    val x: Int
}

expect abstract class B

expect class C : B

expect abstract class D() {
    val x: Int
}

class E : D()

// MODULE: jvm()()(common)
// FILE: main.kt
interface I {
    val x: Int
}

actual class A : I {
    actual val <!VIRTUAL_MEMBER_HIDDEN!>x<!> = 0
}

actual abstract class B() {
    val x = 0
}

actual class C : B(), I {}

actual abstract class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>D<!> {
    actual val <!REDECLARATION!>x<!> = 0
    val <!ACTUAL_MISSING, REDECLARATION!>x<!> = 0
}
