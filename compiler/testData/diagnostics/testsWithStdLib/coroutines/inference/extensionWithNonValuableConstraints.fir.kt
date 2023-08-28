// !DIAGNOSTICS: -UNUSED_PARAMETER
// !OPT_IN: kotlin.RequiresOptIn
// NI_EXPECTED_FILE

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.ExperimentalTypeInference

interface Base

interface Controller<T> : Base {
    suspend fun yield(t: T) {}
}

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()

fun Base.baseExtension() {}
fun Controller<out Any?>.outNullableAnyExtension() {}
fun Controller<out Any>.outAnyExtension() {}
fun Controller<Any?>.invNullableAnyExtension() {}
fun <S> Controller<S>.genericExtension() {}

fun Controller<String>.safeExtension() {}

val test1 = generate {
    yield("foo")
    baseExtension()
}

val test2 = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    baseExtension()
}

val test3 = generate {
    yield(42)
    outNullableAnyExtension()
}

val test4 = generate {
    outNullableAnyExtension()
}

val test5 = generate {
    yield(42)
    outAnyExtension()
}

val test6 = generate {
    yield("bar")
    invNullableAnyExtension()
}

val test7 = <!NEW_INFERENCE_ERROR!>generate {
    yield("baz")
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>genericExtension<!><Int>()
}<!>

val test8 = generate {
    safeExtension()
}
