// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common()()()
// FILE: common.kt
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> class Foo {
    class Inner
    class <!NO_ACTUAL_FOR_EXPECT{JVM}!>AbsentOnactual<!>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner
}

actual typealias <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> = FooImpl
