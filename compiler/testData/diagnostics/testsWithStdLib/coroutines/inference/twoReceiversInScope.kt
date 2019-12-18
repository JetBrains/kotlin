// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface MyFlow<out T>
interface MyFlowCollector<in T>

fun <F> flow(@BuilderInference block: MyFlowCollector<F>.() -> Unit): MyFlow<F> = TODO()

interface SendChannel<in E> {
    fun send(element: E)
}

fun <P> produce(@BuilderInference block: SendChannel<P>.() -> Unit) {}

fun <C> MyFlow<C>.collect(action: (C) -> Unit) {}

private fun <T> MyFlow<T>.idScoped(): MyFlow<T> {
    return flow {
        produce {
            collect {
                send(it)
            }
        }
    }
}