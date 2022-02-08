// !OPT_IN: kotlin.RequiresOptIn
// WITH_STDLIB

// ISSUE: KT-35684

import kotlin.experimental.ExperimentalTypeInference

fun test() {
    sequence {
        yield(materialize())
        yield(materialize<Int>())
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <U> sequence(@BuilderInference block: suspend Inv<U>.() -> Unit) {}

interface Inv<T> {
    fun yield(element: T)
}

fun <K> materialize(): Inv<K> = TODO()

fun box(): String = "OK"
