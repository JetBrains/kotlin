// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

import kotlin.coroutines.*

fun ordinal() {
    kotlin.coroutines.coroutineContext
    coroutineContext
}

suspend fun named() {
    kotlin.coroutines.coroutineContext
    coroutineContext
}

class A {
    val coroutineContextNew = kotlin.coroutines.coroutineContext
    val context = coroutineContext
}

class Controller {
    fun ordinal() {
        kotlin.coroutines.coroutineContext
        coroutineContext
    }

    suspend fun named() {
        kotlin.coroutines.coroutineContext
        coroutineContext
    }

    suspend fun severalArgs(s: String, a: Any) {
        kotlin.coroutines.coroutineContext
        coroutineContext
    }
}

fun builder(c: () -> CoroutineContext) = {}
fun builderSuspend(c: suspend () -> CoroutineContext) = {}

fun builderSeveralArgs(c: (Int, Int, Int) -> CoroutineContext) = {}
fun builderSuspendSeveralArgs(c: suspend (Int, Int, Int) -> CoroutineContext) = {}

fun test() {
    builder { kotlin.coroutines.coroutineContext }
    builder { coroutineContext }
    builderSuspend { kotlin.coroutines.coroutineContext }
    builderSuspend { coroutineContext }
    builderSeveralArgs { _, _, _ -> kotlin.coroutines.coroutineContext }
    builderSeveralArgs { _, _, _ -> coroutineContext }
    builderSuspendSeveralArgs { _, _, _ -> kotlin.coroutines.coroutineContext }
    builderSuspendSeveralArgs { _, _, _ -> coroutineContext }
}
