// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!INCOMPATIBLE_MATCHING{JVM}!>expect class A {
    <!INCOMPATIBLE_MATCHING{JVM}!>class B {
        @Ann
        fun foo()
        <!NO_ACTUAL_FOR_EXPECT{JVM}!>fun missingOnActual()<!>
    }<!>
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class AImpl {
    class B {
        fun foo() {}
    }
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT, NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> = AImpl<!>
