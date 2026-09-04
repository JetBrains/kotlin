// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun foo(): Int = 1

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun foo(): Int = 2

fun box(): String = if (foo() == 2) "OK" else "FAIL"
