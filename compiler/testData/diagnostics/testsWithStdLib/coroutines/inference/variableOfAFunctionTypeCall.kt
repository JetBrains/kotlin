// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface ProducerScope<E> {
    fun yield(e: E)
}

@OptIn(ExperimentalTypeInference::class)
fun <E> produce(@BuilderInference block: ProducerScope<E>.() -> Unit): ProducerScope<E> = TODO()

fun <K> filter(e: K, predicate: (K) -> Boolean) =
    produce {
        predicate(e)
        yield(42)
    }