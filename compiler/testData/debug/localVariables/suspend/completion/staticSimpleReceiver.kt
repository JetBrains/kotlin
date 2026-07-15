

// FILE: test.kt
class A

suspend fun A.foo() {}

suspend fun box() {
    A().foo()
}


// EXPECTATIONS JVM_IR
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:4 <init>:
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=Generated_Box_MainKt$main$1

// EXPECTATIONS JS_IR
// test.kt:10 box: $completion=EmptyContinuation
// test.kt:4 <init>:
// test.kt:9 box: $completion=EmptyContinuation
// test.kt:6 foo: <this>=A, $completion=EmptyContinuation

// EXPECTATIONS WASM
// test.kt:9 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (4, 4)
// test.kt:4 $A.<init>: $<this>:(ref $A)=(ref $A) (7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)
// test.kt:9 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (4, 8)
// test.kt:6 $foo: $<this>:(ref $A)=(ref $A), $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (21, 21)
// test.kt:9 $box: $$completion:(ref $EmptyContinuation)=(ref $EmptyContinuation) (8, 8)
