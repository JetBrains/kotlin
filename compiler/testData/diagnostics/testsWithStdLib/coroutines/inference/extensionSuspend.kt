// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

class GenericController<T> {
    suspend fun yield(t: T) {}
}

@BuilderInference
suspend fun <K> GenericController<K>.yieldAll(s: Collection<K>) {}

fun <S> generate(@BuilderInference g: suspend GenericController<S>.() -> Unit): S = TODO()

val test1 = generate {
    yield(4)
    yieldAll(setOf(4, 5))
}

val test2 = generate {
    yieldAll(setOf(B))
}

val test3 = generate {
    yieldAll(setOf(B, C))
}

val test4 = generate {
    yieldAll(setOf(B))

    yield(C)
}



// Utils
fun <X> setOf(vararg x: X): Set<X> = TODO()

interface A
object B : A
object C : A