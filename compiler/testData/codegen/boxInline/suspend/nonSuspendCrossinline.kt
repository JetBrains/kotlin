// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE

// FILE: inline.kt

class MyDeferred<T>(val t: suspend () -> T) {
    suspend fun await() = t()
}

fun <T> async(block: suspend () -> T) = MyDeferred(block)

inline fun <T, R> map(source: MyDeferred<T>, crossinline mapper: (T) -> R) =
    async {
        mapper(source.await())
    }

inline fun <T, R1, R2> map2(source: MyDeferred<T>, crossinline mapper1: (T) -> R1, crossinline mapper2: (R1) -> R2) =
    async {
        val c = suspend {
            mapper1(source.await())
        }
        mapper2(c())
    }

inline fun <T, R1, R2, R3> map3(source: MyDeferred<T>, crossinline mapper1: (T) -> R1, crossinline mapper2: (R1) -> R2, crossinline mapper3: (R2) -> R3) =
    async {
        val c = suspend {
            val c = suspend {
                mapper1(source.await())
            }
            mapper2(c())
        }
        mapper3(c())
    }

// FILE: box.kt

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val source = MyDeferred { 1 }
    var result = -1
    builder {
        result = map(source) { it + 2 }.await()
    }
    if (result != 3) return "FAIL 1 $result"
    builder {
        result = map2(source, { it + 2 }, { it + 3 }).await()
    }
    if (result != 6) return "FAIL 2 $result"
    builder {
        result = map3(source, { it + 2 }, { it + 3 }, { it + 4 }).await()
    }
    if (result != 10) return "FAIL 3 $result"
    return "OK"
}
