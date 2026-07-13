// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: main.kt
package usage

import test.Result

@Suppress("UNCHECKED_CAST")
fun handle(result: Result) {
    val success = result as test.Result.Success<<expr>test.Payload</expr>>
}

// FILE: dependency.kt
package test

sealed class Result {
    class Success<T> : Result()
    class Failure : Result()
}

class Payload
