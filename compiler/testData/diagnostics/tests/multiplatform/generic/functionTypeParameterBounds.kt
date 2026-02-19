// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect fun <T : Comparable<T>> Array<out T>.sort(): Unit

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun <T : Comparable<T>> Array<out T>.sort(): Unit {}

fun <T> Array<out T>.sort(): Unit {}

/* GENERATED_FIR_TAGS: actual, expect, funWithExtensionReceiver, functionDeclaration, nullableType, outProjection,
typeConstraint, typeParameter */
