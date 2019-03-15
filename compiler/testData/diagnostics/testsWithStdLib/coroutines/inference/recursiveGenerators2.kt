// !USE_EXPERIMENTAL: kotlin.Experimental
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

@file:UseExperimental(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

@BuilderInference
suspend fun <S> GenericController<List<S>>.yieldGenerate(g: suspend GenericController<S>.() -> Unit): Unit = TODO()

val test1 = generate {
    // TODO: KT-15185
    <!OI;TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR, OI;TYPE_MISMATCH!>yieldGenerate<!> {
        yield(4)
    }
}