// FIR_IDENTICAL
// IGNORE_BACKEND_K1: ANY
// ^^^ K1 as well as K1-based test infra do not support "fragment refinement".

// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect fun f(): String

// MODULE: platform()()(common)
// FILE: platform.kt

actual fun f(): String = "OK"
