// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T>

fun <S> generate(g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

suspend fun GenericController<List<String>>.test() {}

val test1 = generate {
    test()
}