// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class A {
    class B {
        @Ann
        fun foo()
        fun missingOnActual()
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class AImpl {
    class B {
        fun foo() {}
    }
}

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> = AImpl
