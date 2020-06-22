// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface Base

interface Controller<T> : Base {
    suspend fun yield(t: T) {}
}

fun <S> generate(@BuilderInference g: suspend Controller<S>.() -> Unit): S = TODO()

fun Base.baseExtension() {}
fun Controller<out Any?>.outNullableAnyExtension() {}
fun Controller<out Any>.outAnyExtension() {}
fun Controller<Any?>.invNullableAnyExtension() {}
fun <S> Controller<S>.genericExtension() {}

@BuilderInference
fun Controller<String>.safeExtension() {}

val test1 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>("foo")
    baseExtension()
}

val test2 = generate {
    baseExtension()
}

val test3 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>(42)
    outNullableAnyExtension()
}

val test4 = generate {
    outNullableAnyExtension()
}

val test5 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>(42)
    outAnyExtension()
}

val test6 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>("bar")
    <!INAPPLICABLE_CANDIDATE!>invNullableAnyExtension<!>()
}

val test7 = generate {
    <!INAPPLICABLE_CANDIDATE!>yield<!>("baz")
    <!INAPPLICABLE_CANDIDATE!>genericExtension<!><Int>()
}

val test8 = generate {
    <!INAPPLICABLE_CANDIDATE!>safeExtension<!>()
}