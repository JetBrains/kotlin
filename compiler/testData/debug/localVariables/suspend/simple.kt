// WITH_COROUTINES
// FILE: test.kt

suspend fun box() {
    var x = 1
}

// LOCAL VARIABLES
// test.kt:5 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// test.kt:6 box: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation, x:int=1:int