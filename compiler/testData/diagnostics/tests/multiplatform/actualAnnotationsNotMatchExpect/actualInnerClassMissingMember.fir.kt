// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
annotation class Ann

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class A {
    class <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>B<!> {
        @Ann
        fun foo()
        fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>missingOnActual<!>()
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class AImpl {
    class B {
        fun foo() {}
    }
}

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> = AImpl
