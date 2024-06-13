// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// OPT_IN: kotlin.RequiresOptIn

import kotlin.experimental.ExperimentalTypeInference

interface ProducerScope<E> {
    fun yield(e: E)
}

@OptIn(ExperimentalTypeInference::class)
fun <E> produce(block: ProducerScope<E>.() -> Unit): ProducerScope<E> = TODO()

fun <K> filter(e: K, predicate: (K) -> Boolean) =
    produce {
        predicate(e)
        yield(42)
    }
