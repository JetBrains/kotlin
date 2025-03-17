// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// LENIENT_MODE
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect fun foo()
expect fun bar(): Any?
expect fun baz(): String
expect fun qux(): Any

// MODULE: platform()()(common)
// FILE: platform.kt
fun main() {}