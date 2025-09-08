// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

expect fun f1(s: () -> String)
expect inline fun f2(s: () -> String)
expect inline fun f3(noinline s: () -> String)

expect fun f4(s: () -> String)
expect inline fun f5(s: () -> String)
expect inline fun f6(crossinline s: () -> String)

expect fun f7(x: Any)
expect fun f8(vararg x: Any)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun f1(noinline s: () -> String) {}
actual inline fun <!EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_NOINLINE!>f2<!>(noinline s: () -> String) {}
actual inline fun f3(s: () -> String) {}
actual inline fun f4(crossinline s: () -> String) {}
actual inline fun <!EXPECT_ACTUAL_INCOMPATIBLE_VALUE_PARAMETER_CROSSINLINE!>f5<!>(crossinline s: () -> String) {}
actual inline fun f6(s: () -> String) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>f7<!>(vararg x: Any) {}
actual fun <!ACTUAL_WITHOUT_EXPECT!>f8<!>(x: Any) {}

/* GENERATED_FIR_TAGS: actual, crossinline, expect, functionDeclaration, functionalType, inline, noinline, vararg */
