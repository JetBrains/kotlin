// WITH_STDLIB

import kotlin.coroutines.*

// The stdlib's `Continuation` function regenerates an anonymous object compiled for Java 8, where it is an identity class.
fun box(): String {
    val continuation = Continuation<Unit>(EmptyCoroutineContext) {}
    return if (continuation.javaClass.isValue) "Fail: ${continuation.javaClass} is a value class" else "OK"
}
