// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
suspend fun box() {}


// EXPECTATIONS
// test.kt:4 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
