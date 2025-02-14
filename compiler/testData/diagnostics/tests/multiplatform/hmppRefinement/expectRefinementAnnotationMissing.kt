// FIR_IDENTICAL
// SKIP_K1
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common1
expect fun foo()

// MODULE: common2()()(common1)
expect fun foo()

// MODULE: main()()(common2)
actual fun foo() {}
