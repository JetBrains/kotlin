// IGNORE_BACKEND_K2: ANY
// !LANGUAGE: +MultiPlatformProjects
// SKIP_KLIB_TEST

expect sealed class Ops()
expect class Add() : Ops

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()
