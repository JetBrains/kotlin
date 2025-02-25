// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
expect val x1: Int

expect val x2: Int

expect val x3: Int

// MODULE: intermediate()()(common)
expect val x1: Int

val <!ACTUAL_MISSING!>x2<!> = 2

actual val x3 = 3

// MODULE: main()()(intermediate)
actual val x1 = 1

actual val x2 = 2

val <!ACTUAL_MISSING!>x3<!> = 3
