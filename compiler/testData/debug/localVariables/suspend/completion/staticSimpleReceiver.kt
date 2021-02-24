
// WITH_COROUTINES
// FILE: test.kt
class A

suspend fun A.foo() {}

suspend fun box() {
    A().foo()
}


// LOCAL VARIABLES
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:4 <init>:
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:6 foo: $this$foo:A=A, $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation