// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

fun <T> a(): T = TODO()
val b: Unit = throw a()