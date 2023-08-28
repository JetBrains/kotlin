// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
// !LANGUAGE: +MultiPlatformProjects
// SKIP_KLIB_TEST

expect sealed class Ops()
expect class Add() : Ops

actual sealed class Ops actual constructor()
actual class Add actual constructor() : Ops()
