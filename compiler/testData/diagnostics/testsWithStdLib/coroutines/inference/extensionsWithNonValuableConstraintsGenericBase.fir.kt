// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface Base<K>

interface Controller<T> : Base<T> {
    suspend fun yield(t: T) {}
}

interface SpecificController<T> : Base<String> {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend Controller<S>.() -> Unit): S = TODO()
fun <S> generateSpecific(@BuilderInference g: suspend SpecificController<S>.() -> Unit): S = TODO()

fun Base<*>.starBase() {}
fun Base<String>.stringBase() {}

val test1 = generate {
    starBase()
    <!INAPPLICABLE_CANDIDATE!>yield<!>("foo")
}

val test2 = generate {
    starBase()
}

val test3 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>("bar")
    <!INAPPLICABLE_CANDIDATE!>stringBase<!>()
}

val test4 = generateSpecific {
    <!INAPPLICABLE_CANDIDATE!>yield<!>(42)
    starBase()
}

val test5 = generateSpecific {
    <!INAPPLICABLE_CANDIDATE!>yield<!>(42)
    stringBase()
}

val test6 = generateSpecific {
    stringBase()
}