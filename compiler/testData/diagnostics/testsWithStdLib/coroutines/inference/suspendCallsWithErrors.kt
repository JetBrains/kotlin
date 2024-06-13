// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class Controller<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

class A

val test1 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(<!NO_COMPANION_OBJECT!>A<!>)
}

val test2: Int = generate {
    yield(<!TYPE_MISMATCH!>A()<!>)
}
