// LANGUAGE: +MultiPlatformProjects

// MODULE: common0
// FILE: common0.kt

expect fun f0(s: S): S

expect class S

// MODULE: common1()()(common0)
// FILE: common1.kt

actual fun f0(s: S): S = s

// MODULE: jvm()()(common1)
// FILE: jvm.kt

actual typealias S = String

fun box() = f0("OK")
