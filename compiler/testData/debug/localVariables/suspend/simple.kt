// WITH_COROUTINES
// FILE: test.kt

suspend fun box() {
    var x = 1
}

// LOCAL VARIABLES
// TestKt:5: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation
// TestKt:6: $completion:kotlin.coroutines.Continuation=helpers.ResultContinuation, x:int=1:int