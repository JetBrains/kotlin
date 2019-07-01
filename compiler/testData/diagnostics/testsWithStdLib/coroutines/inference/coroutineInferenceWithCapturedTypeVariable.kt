// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.Experimental

import kotlin.experimental.ExperimentalTypeInference

suspend fun main() {
    iFlow { emit(1) }
}


@UseExperimental(ExperimentalTypeInference::class)
fun <K> iFlow(@BuilderInference block: suspend iFlowCollector<in K>.() -> Unit): iFlow<K> = TODO()

interface iFlowCollector<S> {
    suspend fun emit(value: S)
}

interface iFlow<out V>