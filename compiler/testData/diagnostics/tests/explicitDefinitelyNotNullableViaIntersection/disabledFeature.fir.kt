// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -DefinitelyNonNullableTypes

fun <T> foo(x: T, y: T & Any): List<T & Any>? = null
