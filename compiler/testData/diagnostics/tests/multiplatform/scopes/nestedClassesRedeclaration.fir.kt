// LANGUAGE: +MultiPlatformProjects
// DONT_STOP_ON_FIR_ERRORS

// MODULE: common
// FILE: common.kt
expect class A {
    class N
}

expect class B {}

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect class C {
    <!NO_ACTUAL_FOR_EXPECT{JVM}!>class N<!>
}<!>

expect abstract class D()

class E : D() {
    class N
}

// MODULE: jvm()()(common)
// FILE: main.kt
abstract class P() {
    class N
}

actual class A : P() {
    actual class N
}

actual class B : P() {
    class N
}

actual class C {
    actual class <!REDECLARATION!>N<!>
    class <!ACTUAL_MISSING, REDECLARATION!>N<!>
}

actual abstract class D {
    class N
}
