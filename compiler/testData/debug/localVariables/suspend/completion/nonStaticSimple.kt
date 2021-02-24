
// WITH_COROUTINES
// FILE: test.kt
class A {
    suspend fun foo() {}
}

suspend fun box() {
    A().foo()
}

// LOCAL VARIABLES
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:4 <init>:
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:5 foo: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:9 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:10 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation