// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface A
<!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> fun <T : A> foo(t: T): String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun <T : A> <!ACTUAL_MISSING!>foo<!>(t: T): T = TODO()
fun <T> foo(t: T): String = TODO()
