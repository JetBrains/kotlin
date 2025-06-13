// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER
// OPT_IN: kotlin.RequiresOptIn
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

fun <S> generate(g: suspend Controller<S>.() -> Unit): S = TODO()
fun <S> generateSpecific(g: suspend SpecificController<S>.() -> Unit): S = TODO()

fun Base<*>.starBase() {}
fun Base<String>.stringBase() {}

val test1 = generate {
    starBase()
    yield("foo")
}

val test2 = <!CANNOT_INFER_PARAMETER_TYPE!>generate<!> {
    starBase()
}

val test3 = generate {
    yield("bar")
    stringBase()
}

val test4 = generateSpecific {
    yield(42)
    starBase()
}

val test5 = generateSpecific {
    yield(42)
    stringBase()
}

val test6 = <!CANNOT_INFER_PARAMETER_TYPE!>generateSpecific<!> {
    stringBase()
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, funWithExtensionReceiver, functionDeclaration,
functionalType, integerLiteral, interfaceDeclaration, lambdaLiteral, nullableType, propertyDeclaration, starProjection,
stringLiteral, suspend, typeParameter, typeWithExtension */
