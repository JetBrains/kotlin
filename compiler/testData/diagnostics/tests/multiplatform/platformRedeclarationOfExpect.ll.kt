// DONT_STOP_ON_FIR_ERRORS
// MODULE: common
// FILE: common.kt
expect class Foo

// MODULE: main()()(common)
// FILE: test.kt
expect class Foo
