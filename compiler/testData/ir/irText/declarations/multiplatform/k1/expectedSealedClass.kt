// IGNORE_BACKEND_K2: ANY
// ^^^ In FIR, declaring the same `expect` and `actual` classes in one compiler module is not possible (see KT-55177).

// !LANGUAGE: +MultiPlatformProjects
// SKIP_KLIB_TEST

expect sealed class Ops()
expect class Add() : Ops

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()
