// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-35684

import kotlin.experimental.ExperimentalTypeInference

fun test_1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>sequence {
        yield(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Int>")!>materialize()<!>)
        yield(materialize<Int>())
    }<!>
}

fun test_2() {
    sequence {
        yield(materialize())
    }
}

fun test_3() {
    sequence {
        yield(materialize<Int>())
        materialize()
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <U> sequence(@BuilderInference block: suspend Inv<U>.() -> Unit): U = null!!

interface Inv<T> {
    fun yield(element: T)
}

fun <K> materialize(): Inv<K> = TODO()