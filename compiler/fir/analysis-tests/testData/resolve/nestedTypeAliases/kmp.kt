// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects

// MODULE: common

// FILE: expect1.kt

expect class E1 {
    class I
}

// FILE: expect2.kt

expect class E2 {
    class I
}

// FILE: expect3.kt

expect class E3 {
    class I
}

// MODULE: platform()()(common)

// FILE: actual1.kt

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("actual class E1 : Any;     expect class I : Any")!>E1<!> {
    actual typealias I = Int  // 'actual typealias' not allowed
}

// FILE: actual2.kt

class A {
    typealias I = Int
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS("actual typealias E2 = A;     expect class I : Any")!>E2<!> = A  // actualizing nested 'expect class' with typealias not allowed

// FILE: actual3.kt

class B {
    class I
}

actual typealias E3 = B  // OK

