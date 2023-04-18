// IGNORE_BACKEND_K1: JS, JS_IR, JS_IR_ES6, NATIVE
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-57263

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect val traceFormatDefault: String
expect fun Trace(format: String = traceFormatDefault): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual val traceFormatDefault: String = ""
actual fun Trace(format: String) = format

fun box() = Trace("OK")