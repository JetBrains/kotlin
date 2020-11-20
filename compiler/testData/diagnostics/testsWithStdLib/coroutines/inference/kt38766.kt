// FIR_IDENTICAL
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.experimental.ExperimentalTypeInference

inline fun <reified T, R> emptyFlow(crossinline transform: suspend (Array<T>) -> R): Flow1<R> =
    flow1 { emit(transform(emptyArray())) }

inline fun <reified T, R> emptyFlow(crossinline transform: (Array<T>) -> R): Flow1<R> =
    flowOf1(transform(emptyArray()))

fun <T> flowOf1(value: T): Flow1<T> = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <T> flow1(@BuilderInference block: suspend FlowCollector1<T>.() -> Unit): Flow1<T> = TODO()

interface FlowCollector1<in T> {
    suspend fun emit(value: T)
}

interface Flow1<out T>