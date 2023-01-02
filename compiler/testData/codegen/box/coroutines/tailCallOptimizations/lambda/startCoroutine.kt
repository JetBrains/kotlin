// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result = ""

    suspend {
        result = "OK"
    }.startCoroutine(EmptyContinuation)

    return result
}
