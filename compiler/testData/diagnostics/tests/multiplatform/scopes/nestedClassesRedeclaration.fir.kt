// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class A {
    class N
}

expect class B {}

expect class C {
    <!AMBIGUOUS_ACTUALS{JVM}!>class N<!>
}

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
