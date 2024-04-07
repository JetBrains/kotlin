// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt
expect class <!NO_ACTUAL_FOR_EXPECT!>A<!> {
    class N
}

expect class <!NO_ACTUAL_FOR_EXPECT!>B<!> {}

expect class <!NO_ACTUAL_FOR_EXPECT!>C<!> {
    class N
}

expect abstract class <!NO_ACTUAL_FOR_EXPECT!>D<!>()

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
