// !OPT_IN: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

@BuilderInference
suspend fun <S> GenericController<List<S>>.yieldGenerate(g: suspend GenericController<S>.() -> Unit): Unit = TODO()

val test1 = generate {
    // TODO: KT-15185
    yieldGenerate {
        yield(4)
    }
}
