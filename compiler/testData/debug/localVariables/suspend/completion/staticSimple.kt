// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
suspend fun box() {}


// EXPECTATIONS JVM JVM_IR
// test.kt:4 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:4 box: $completion=EmptyContinuation
