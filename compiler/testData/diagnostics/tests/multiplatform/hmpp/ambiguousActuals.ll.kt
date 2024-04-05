// DONT_STOP_ON_FIR_ERRORS
// MODULE: common
// TARGET_PLATFORM: Common
expect fun foo()

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common
actual fun foo() {}

// MODULE: main()()(common, intermediate)
actual fun foo() {}
