// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
// FILE: common.kt
expect class Foo

// MODULE: main()()(common)
// FILE: test.kt
expect class Foo
