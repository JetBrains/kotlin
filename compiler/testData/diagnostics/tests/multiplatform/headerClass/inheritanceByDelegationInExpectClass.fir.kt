// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface A

class B : A
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class Foo(b: B) : <!IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS!>A by b<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class Bar : <!IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS!>A by B()<!>

// MODULE: m1-jvm()()(m1-common)
