// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_COROUTINES
// WITH_STDLIB
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface Z {
    private suspend fun test(): String {
        return "OK"
    }

    suspend fun test2(): String {
        return test()
    }
}

fun box(): String {
    var result = "fail"
    builder {
        result = object : Z {}.test2()
    }
    return result
}