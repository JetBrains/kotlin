// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-69773
// WITH_STDLIB

interface MyList<out E> : List<E>

fun test(a: List<Any?>) = a is MyList
