// WITH_STDLIB
// FILE: test.kt

suspend fun box() {
    var x = 1
}

// EXPECTATIONS JVM_IR
// test.kt:5 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1, x:int=1:int

// EXPECTATIONS JS_IR
// test.kt:5 box: $completion=EmptyContinuation
// test.kt:6 box: $completion=EmptyContinuation, x=1:number
