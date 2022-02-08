// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-33545

import kotlin.experimental.ExperimentalTypeInference

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

inline fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit): Unit {}

abstract class LiveData<T>

interface LiveDataScope<T> {
    suspend fun emit(value: T)
}

@OptIn(ExperimentalTypeInference::class)
fun <T> liveData(@BuilderInference block: suspend LiveDataScope<T>.() -> Unit): LiveData<T> = null!!

fun <Value> Flow<Value>.asLiveData() = liveData {
    collect(this::emit)
//    collect { emit(it) }
}

fun box(): String = "OK"
