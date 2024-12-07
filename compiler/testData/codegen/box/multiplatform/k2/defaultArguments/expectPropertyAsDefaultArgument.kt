// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-57263

// MODULE: common
// FILE: common.kt

expect val traceFormatDefault: String
expect fun Trace(format: String = traceFormatDefault): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual val traceFormatDefault: String = ""
actual fun Trace(format: String) = format

fun box() = Trace("OK")
