// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: common
expect fun foo()
expect class Foo

// MODULE: intermediate()()(common)
actual fun foo() {}
actual class Foo

// MODULE: main()()(common, intermediate)
actual fun foo() {}
actual class Foo
