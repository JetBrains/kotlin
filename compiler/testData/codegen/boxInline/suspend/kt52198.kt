// WITH_COROUTINES
// WITH_STDLIB
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM, JVM_MULTI_MODULE_IR_AGAINST_OLD
// FILE: lib.kt
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun foo(value: String): String {
    var x = "fail"
    suspendCoroutineUninterceptedOrReturn<Unit> {
        x = value
        it.resume(Unit)
        COROUTINE_SUSPENDED
    }
    return x
}

suspend inline fun fooInline(value: String): String {
    var x = "fail"
    suspendCoroutineUninterceptedOrReturn<Unit> {
        x = value
        it.resume(Unit)
        COROUTINE_SUSPENDED
    }
    return x
}

// FILE: main.kt
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    var result = ""
    suspend {
        result = foo("O") + fooInline("K")
    }.startCoroutine(EmptyContinuation)
    return result
}
