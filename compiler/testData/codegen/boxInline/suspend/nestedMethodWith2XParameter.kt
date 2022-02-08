// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM
// FILE: a.kt

inline suspend fun f(crossinline lambda: suspend (Double) -> Double): Double {
    val obj = object {
        suspend fun g(x: Double): Double = lambda(x)
    }
    return obj.g(1.0)
}

// FILE: b.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result: Double = 0.0
    suspend { result = f { it } }.startCoroutine(EmptyContinuation)
    return if (result == 1.0) "OK" else "fail: $result"
}
