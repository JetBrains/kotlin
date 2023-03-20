// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
class A

suspend fun A.foo() {}

suspend fun box() {
    A().foo()
}


// EXPECTATIONS JVM JVM_IR
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:9 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:9 box: $completion=EmptyContinuation
// test.kt:6 foo: <this>=A, $completion=EmptyContinuation
