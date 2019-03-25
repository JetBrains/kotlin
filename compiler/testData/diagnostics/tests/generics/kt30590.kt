// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
// !LANGUAGE: +NewInference
// Issue: KT-30590

interface A
fun <T: A, R: T> emptyStrangeMap(): Map<T, R> = TODO()
fun test7() : Map<A, A> = emptyStrangeMap()

fun test() = emptyStrangeMap()

