// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T>

@BuilderInference
suspend fun <S> GenericController<S>.yieldAll(s: Collection<S>): String = ""
@BuilderInference
suspend fun <S> GenericController<S>.yieldAll(s: Set<S>): Int = 4

fun <T, R> generate(@BuilderInference g: suspend GenericController<T>.() -> R): Pair<T, R> = TODO()

val test1 = generate {
    yieldAll(setOf(4))
}

val test2 = generate {
    yieldAll(listOf(4))
}

// Util function
fun <X> setOf(vararg x: X): Set<X> = TODO()
fun <X> listOf(vararg x: X): List<X> = TODO()
class Pair<T, S>
