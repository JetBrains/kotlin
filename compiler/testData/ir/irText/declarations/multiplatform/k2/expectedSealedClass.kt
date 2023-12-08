// IGNORE_BACKEND_K1: ANY
// ^^^ K1 as well as K1-based test infra do not support "fragment refinement".

// FIR_IDENTICAL
// !LANGUAGE: +MultiPlatformProjects
// SKIP_KLIB_TEST

// MODULE: common
// FILE: common.kt

expect sealed class Ops()
expect class Add() : Ops

// MODULE: platform()()(common)
// FILE: platform.kt

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()
