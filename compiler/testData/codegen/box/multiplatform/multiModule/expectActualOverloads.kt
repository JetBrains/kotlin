// TARGET_BACKEND: JVM_IR
// !LANGUAGE: +MultiPlatformProjects
// MODULE: common
// TARGET_PLATFORM: Common
// FILE: commonMain.kt

expect class S

expect fun foo(s: S): S

expect fun foo(i: Int): Int

fun test(s: S) = foo(s)

// MODULE: main()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual fun foo(i: Int) = i

actual fun foo(s: String) = s

actual typealias S = String

fun box() = test("OK")