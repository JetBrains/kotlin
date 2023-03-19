// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// Code generation problem with JVM backend.
// IGNORE_BACKEND: JVM
// FILE: test.kt
class A

suspend fun A.foo() {}

suspend fun box() {
    A().foo()
}


// EXPECTATIONS JVM JVM_IR
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 <init>:
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:8 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:11 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:12 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:11 box: $completion=EmptyContinuation
// test.kt:6 <init>:
// test.kt:11 box: $completion=EmptyContinuation
// test.kt:8 foo: <this>=A, $completion=EmptyContinuation
