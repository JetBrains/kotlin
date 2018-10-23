// !DIAGNOSTICS: -UNUSED_PARAMETER
// !USE_EXPERIMENTAL: kotlin.Experimental

@file:UseExperimental(ExperimentalTypeInference::class)

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
    yield("foo")
    baseExtension()
}

val test2 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    baseExtension()
}

val test3 = generate {
    yield(42)
    outNullableAnyExtension()
}

val test4 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    outNullableAnyExtension()
}

val test5 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield(42)
    outAnyExtension()
}

val test6 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield("bar")
    invNullableAnyExtension()
}

val test7 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>generate<!> {
    yield("baz")
    genericExtension<Int>()
}

val test8 = generate {
    safeExtension()
}