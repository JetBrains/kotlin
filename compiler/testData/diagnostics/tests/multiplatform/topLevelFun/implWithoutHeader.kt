// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: jvm.kt

actual fun <!ACTUAL_WITHOUT_EXPECT{JVM}!>foo<!>() { }

// MODULE: m1-jvm()()(m1-common)
