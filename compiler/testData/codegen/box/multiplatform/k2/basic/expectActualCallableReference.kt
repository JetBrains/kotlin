// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class R

expect fun ret(): R

fun foo() = ::ret

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun ret(): R = "OK"

actual typealias R = String

fun box() = foo().invoke()
