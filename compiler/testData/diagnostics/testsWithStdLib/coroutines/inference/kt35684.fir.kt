// !LANGUAGE: +NewInference
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !DIAGNOSTICS: -UNUSED_PARAMETER
// ISSUE: KT-35684

import kotlin.experimental.ExperimentalTypeInference

fun test_1() {
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing")!>sequence {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(<!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Any?>")!>materialize()<!>)
        <!INAPPLICABLE_CANDIDATE!>yield<!>(materialize<Int>())
    }<!>
}

fun test_2() {
    sequence {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(materialize())
    }
}

fun test_3() {
    sequence {
        <!INAPPLICABLE_CANDIDATE!>yield<!>(materialize<Int>())
        materialize()
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <U> sequence(@BuilderInference block: suspend Inv<U>.() -> Unit): U = null!!

interface Inv<T> {
    fun yield(element: T)
}

fun <K> materialize(): Inv<K> = TODO()