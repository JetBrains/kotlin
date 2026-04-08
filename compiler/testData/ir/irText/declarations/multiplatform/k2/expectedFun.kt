// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun f(): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun f(): String = "OK"
