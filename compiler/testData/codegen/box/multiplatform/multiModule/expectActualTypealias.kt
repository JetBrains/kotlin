// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class S {
    val length: Int
}

expect fun foo(): S

fun test(): S = foo()

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual typealias S = String

actual fun foo(): S = "OK"

fun box() = test()