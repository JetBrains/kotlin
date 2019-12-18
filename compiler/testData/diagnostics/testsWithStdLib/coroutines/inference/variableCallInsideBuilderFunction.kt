// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn

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
