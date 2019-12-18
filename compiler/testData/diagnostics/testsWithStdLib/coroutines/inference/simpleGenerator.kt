// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
    suspend fun yieldSet(t: Set<T>) {}
    suspend fun yieldVararg(vararg t: T) {}
}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): S = TODO()

val test1 = generate {
    yield(4)
}

val test2 = generate {
    yieldSet(setOf(1, 2, 3))
}

val test3 = generate {
    yieldVararg(1, 2, 3)
}

val test4 = generate {
    yieldVararg(1, 2, "")
}


// Util function
fun <X> setOf(vararg x: X): Set<X> = TODO()