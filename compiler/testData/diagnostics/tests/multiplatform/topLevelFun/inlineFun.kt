// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

inline expect fun inlineFun()
expect fun nonInlineFun()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

<!ACTUAL_WITHOUT_EXPECT!>actual<!> fun inlineFun() { }
actual fun nonInlineFun() { }
