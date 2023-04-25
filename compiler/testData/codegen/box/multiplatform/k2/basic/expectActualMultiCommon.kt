// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common0
// TARGET_PLATFORM: Common
// FILE: common0.kt

expect fun f0(): Boolean

fun g0() = f0()

// MODULE: common1()()(common0)
// TARGET_PLATFORM: Common
// FILE: common1.kt

expect fun f1(): String

fun g1() = f1()

// MODULE: common2()()(common0)
// TARGET_PLATFORM: Common
// FILE: common2.kt

expect fun f2(): String

fun g2() = f2()

// MODULE: platform()()(common1, common2)
// FILE: platform.kt

actual fun f0(): Boolean = true
actual fun f1(): String = "O"
actual fun f2(): String = "K"

fun box() = if (g0()) g1() + g2() else "fail"