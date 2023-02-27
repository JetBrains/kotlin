// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class R

expect fun ret(): R

fun foo() = ::ret

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual fun ret(): R = "OK"

actual typealias R = String

fun box() = foo().invoke()