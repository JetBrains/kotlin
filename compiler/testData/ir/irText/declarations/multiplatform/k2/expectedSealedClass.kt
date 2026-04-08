// FIR_IDENTICAL
// LANGUAGE: +MultiPlatformProjects

// MODULE: common
// FILE: common.kt

expect sealed class Ops()
expect class Add() : Ops

// MODULE: platform()()(common)
// FILE: platform.kt

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()
