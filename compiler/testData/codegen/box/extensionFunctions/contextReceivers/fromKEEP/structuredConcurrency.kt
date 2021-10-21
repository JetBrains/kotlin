// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

interface CoroutineScope
object MyCoroutineScope : CoroutineScope

interface Flow<out T> {
    suspend fun collect(): String
}

inline fun <T : Any> flow(crossinline block: suspend () -> String) = object : Flow<T> {
    override suspend fun collect(): String = block()
}

fun CoroutineScope.launch(c: suspend () -> String): String {
    var result: String = "fail"
    c.startCoroutine(handleResultContinuation { value ->
        result = value
    })
    return result
}

context(CoroutineScope)
fun <T> Flow<T>.launchFlow() = launch { collect() }

fun simpleFlow(): Flow<String> = flow {
    "OK"
}

fun box(): String {
    return with(MyCoroutineScope) {
        simpleFlow().launchFlow()
    }
}