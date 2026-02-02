

// FILE: test.kt
suspend fun box() {}


// EXPECTATIONS JVM_IR
// test.kt:4 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:4 box: $completion=EmptyContinuation

// EXPECTATIONS WASM
// test.kt:4 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (19, 19)
