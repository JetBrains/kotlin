// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
// OPT_IN: kotlin.RequiresOptIn

import kotlin.experimental.ExperimentalTypeInference

suspend fun main() {
    iFlow { emit(1) }
}


@OptIn(ExperimentalTypeInference::class)
fun <K> iFlow(block: suspend iFlowCollector<in K>.() -> Unit): iFlow<K> = TODO()

interface iFlowCollector<S> {
    suspend fun emit(value: S)
}

interface iFlow<out V>