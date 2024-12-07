// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt

inline <!EXPECT_ACTUAL_INCOMPATIBILITY{JVM}!>expect<!> fun inlineFun()
expect fun nonInlineFun()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun <!ACTUAL_WITHOUT_EXPECT!>inlineFun<!>() { }
actual fun nonInlineFun() { }

// MODULE: m3-js()()(m1-common)
// FILE: js.kt

actual <!NOTHING_TO_INLINE!>inline<!> fun inlineFun() { }
actual <!NOTHING_TO_INLINE!>inline<!> fun nonInlineFun() { }
