// WITH_STDLIB
// WITH_COROUTINES
// FILE: 1.kt
package test

inline fun foo(crossinline x: () -> Unit) = suspend {
    try { } finally {
        // This object is regenerated twice (normal return & "catch Throwable, execute finally, and rethrow")
        // It doesn't *need* to be, but this should work regardless.
        { x() }()
    }
}

// FILE: 2.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import test.*

var result = "fail"
fun box(): String {
    suspend {
        foo { result = "OK" }()
    }.startCoroutine(EmptyContinuation)
    return result
}
