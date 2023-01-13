// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect fun func(): String

expect var prop: String

fun test(): String {
    prop = "K"
    return func() + prop
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual fun func(): String = "O"

actual var prop: String = "!"

fun box() = test()