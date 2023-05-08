// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.reflect.full.*


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

interface Flow<T>

class C

suspend fun foo(
    f: suspend (C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C) -> String
): String =
    f(C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C(), C())

fun box(): String {
    var res = "Fail"
    builder {
        val f: suspend (C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C, C) -> String = {
            _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ ->
            "OK"
        }
        res = ::foo.callSuspend(f)
    }
    return res
}
