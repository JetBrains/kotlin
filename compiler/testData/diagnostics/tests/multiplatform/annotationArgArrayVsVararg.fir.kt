// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> annotation class A<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>(vararg val x: String)<!>

@A("abc", "foo", "bar")
fun test() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

// return types are different in expect and actual (Array<out String> in expect vs Array<String> in actual).
// In K1, different return types are mistakenly considered as expect-actual mismatch ("strong incompatibility" in old terminology)
// In K2, different return types are considered as expect-actual incompatibility ("weak incompatibility" in old terminology)
// ACTUAL_MISSING is not reported only when there is a mismatch => K2 is correct
actual annotation class A<!ACTUAL_WITHOUT_EXPECT!>(val <!ACTUAL_MISSING!>x<!>: Array<String>)<!>

@A(<!ARGUMENT_TYPE_MISMATCH!>"abc"<!>, <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test2() {}
