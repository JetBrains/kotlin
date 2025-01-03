// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

interface <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>A<!>
<!CONFLICTING_OVERLOADS!>expect fun <T : A> foo(t: T): <!NO_ACTUAL_FOR_EXPECT{JVM}!>String<!><!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun <T : A> foo(t: T): T = TODO()
fun <T> foo(t: T): String = TODO()
