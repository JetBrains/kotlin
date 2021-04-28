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

val test1 = generate {
    yield(<!NO_COMPANION_OBJECT!>A<!>)
}

val test2: Int = <!INITIALIZER_TYPE_MISMATCH, NEW_INFERENCE_ERROR!>generate {
    yield(A())
}<!>
