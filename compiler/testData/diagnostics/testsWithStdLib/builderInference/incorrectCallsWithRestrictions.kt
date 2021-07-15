// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
    fun notYield(t: T) {}

    suspend fun yieldBarReturnType(t: T) = t
    fun barReturnType(): T = TODO()
}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): List<S> = TODO()

val test1 = generate {
    yield(3)
}

val test2 = generate {
    yield(3)
    notYield(3)
}

val test3 = <!COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE!>generate<!> {
    yield(3)
    yieldBarReturnType(3)
}

val test4 = <!COULD_BE_INFERRED_ONLY_WITH_UNRESTRICTED_BUILDER_INFERENCE!>generate<!> {
    yield(3)
    barReturnType()
}
