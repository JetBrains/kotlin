// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend Controller<S>.() -> Unit): S = TODO()

class A

val test1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}, TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER{OI}!>generate<!> {
    yield(<!NO_COMPANION_OBJECT!>A<!>)
}

val test2: Int = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH{OI}, TYPE_MISMATCH{NI}!>generate {
    yield(<!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>A()<!>)
}<!>
