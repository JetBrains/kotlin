// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JS_IR
// !LANGUAGE: +MultiPlatformProjects
// TODO: K2 JS_IR Fail Reason: IrSimpleFunctionPublicSymbolImpl for kotlin/assertArrayEquals|-1961670457646030164[0] is already bound...

// MODULE: common1
// TARGET_PLATFORM: Common
// FILE: common1.kt

fun o() = "O"

// MODULE: common2
// TARGET_PLATFORM: Common
// FILE: common2.kt

fun k() = "K"

// MODULE: platform()()(common1, common2)
// FILE: platform.kt

fun box() = o() + k()