// WITH_RUNTIME
// WITH_COROUTINES
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.experimental.ExperimentalTypeInference

fun test() {
    flow {
        emit(1)
    }.flatMapLatest<Int, Long> {
        flow {}
    }

    flow {
        emit(1)
    }.flatMap {
        if (it == 1)
            flow {}
        else
            flow<Int> {}
    }

    flow {
        emit(1)
    }.flatMap {
        if (it == 1)
            flow {}
        else flow {}
    }
}

fun <T> Flow<T>.flatMap(mapper: suspend (T) -> Flow<T>): Flow<T> = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <T> flow(@BuilderInference block: suspend FlowCollector<T>.() -> Unit): Flow<T> = TODO()

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R> Flow<T>.flatMapLatest(@BuilderInference crossinline transform: suspend (value: T) -> Flow<R>): Flow<R> = TODO()

interface Flow<out T>

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

fun box(): String {
    return "OK"
}