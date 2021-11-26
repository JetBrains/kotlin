// WITH_STDLIB
// WITH_COROUTINES
// FILE: 1.kt
inline fun <T, R> (suspend () -> T).map(crossinline transform: suspend (T) -> R): suspend () -> R =
    { transform(this()) }

// FILE: 2.kt
import helpers.*
import kotlin.coroutines.*

inline class C(val value: Int)

fun box(): String {
    var result = 0
    suspend {
        result = suspend { C(1) as C? }.map { it }()?.value ?: 2
    }.startCoroutine(EmptyContinuation)
    return if (result == 1) "OK" else "fail: $result"
}
