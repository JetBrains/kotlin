// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun foo(): Int = 1

// MODULE: platform()()(common)
// FILE: platform.kt

fun box(): String = if (foo() == 1) "OK" else "FAIL"
