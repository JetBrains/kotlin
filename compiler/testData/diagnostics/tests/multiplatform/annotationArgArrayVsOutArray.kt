// MODULE: m1-common
// FILE: common.kt

expect annotation class A(val x: Array<out String>)

@A(<!TYPE_MISMATCH, TYPE_MISMATCH{JVM}!>"abc"<!>, <!TOO_MANY_ARGUMENTS, TOO_MANY_ARGUMENTS{JVM}!>"foo"<!>, <!TOO_MANY_ARGUMENTS, TOO_MANY_ARGUMENTS{JVM}!>"bar"<!>)
fun test() {}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

// return types are different in expect and actual (Array<out String> in expect vs Array<String> in actual).
// In K1, different return types are mistakenly considered as expect-actual mismatch ("strong incompatibility" in old terminology)
// In K2, different return types are considered as expect-actual incompatibility ("weak incompatibility" in old terminology)
// ACTUAL_MISSING is not reported only when there is a mismatch => K2 is correct
actual annotation class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!>(val x: Array<String>)

@A(<!TYPE_MISMATCH!>"abc"<!>, <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test2() {}

annotation class B(val x: Array<out String>)

@B(<!TYPE_MISMATCH!>"abc"<!>, <!TOO_MANY_ARGUMENTS!>"foo"<!>, <!TOO_MANY_ARGUMENTS!>"bar"<!>)
fun test3() {}
