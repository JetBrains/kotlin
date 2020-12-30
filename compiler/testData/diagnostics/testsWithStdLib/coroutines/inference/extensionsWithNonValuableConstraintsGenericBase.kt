// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface Base<K>

interface Controller<T> : Base<T> {
    suspend fun yield(t: T) {}
}

interface SpecificController<T> : Base<String> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend Controller<S>.() -> Unit): S = TODO()
fun <S> generateSpecific(@BuilderInference g: suspend SpecificController<S>.() -> Unit): S = TODO()

fun Base<*>.starBase() {}
fun Base<String>.stringBase() {}

val test1 = generate {
    starBase()
    yield("foo")
}

val test2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>generate<!> {
    starBase()
}

val test3 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>generate<!> {
    yield("bar")
    stringBase()
}

val test4 = generateSpecific {
    yield(42)
    starBase()
}

val test5 = generateSpecific {
    yield(42)
    stringBase()
}

val test6 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>generateSpecific<!> {
    stringBase()
}
