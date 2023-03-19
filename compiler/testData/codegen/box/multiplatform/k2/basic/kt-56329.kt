// TARGET_BACKEND: JVM
// !LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-56329

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt

expect class S

expect fun <T> foo(y: T): String

expect fun <T> foo(y: T, x: S): String

fun ok() = foo(1) + foo(2, "K" as S)

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt

actual typealias S = String

actual fun <T> foo(y: T) = "O"

actual fun <T> foo(y: T, x: S) = x

fun box() = ok()