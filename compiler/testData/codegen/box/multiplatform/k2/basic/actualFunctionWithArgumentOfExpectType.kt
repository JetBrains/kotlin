// TARGET_BACKEND: JVM
// LANGUAGE: +MultiPlatformProjects

// MODULE: common0
// TARGET_PLATFORM: Common
// FILE: common0.kt

expect fun f0(s: S): S

expect class S

// MODULE: common1()()(common0)
// TARGET_PLATFORM: Common
// FILE: common1.kt

actual fun f0(s: S): S = s

// MODULE: jvm()()(common1)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt

actual typealias S = String

fun box() = f0("OK")
