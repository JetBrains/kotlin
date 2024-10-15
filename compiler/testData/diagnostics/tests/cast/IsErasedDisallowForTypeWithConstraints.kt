// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
interface A
interface B: A

interface Base<T>

fun <T> test(a: Base<B>) where T: Base<A> = a is <!CANNOT_CHECK_FOR_ERASED!>T<!>