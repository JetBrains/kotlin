// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

expect fun <!NO_ACTUAL_FOR_EXPECT{JVM}!>foo<!>()

// MODULE: m1-jvm()()(m1-common)
