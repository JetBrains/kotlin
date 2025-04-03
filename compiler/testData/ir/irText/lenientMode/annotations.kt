// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: ANY
// LENIENT_MODE
// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
annotation class Ann(vararg val s: String)

@Ann("foo", "bar")
expect fun foo()

@Ann("foo", "bar")
expect class C

// MODULE: platform()()(common)
// FILE: platform.kt
fun main() {}