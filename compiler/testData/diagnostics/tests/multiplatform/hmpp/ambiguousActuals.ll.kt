// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect fun foo()

// MODULE: intermediate()()(common)
actual fun foo() {}

// MODULE: main()()(common, intermediate)
actual fun foo() {}
