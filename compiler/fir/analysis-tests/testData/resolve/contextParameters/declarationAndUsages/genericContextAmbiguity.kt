// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters
// ISSUE: KT-75150

interface Describer<in T> {
    fun describe(t: T): String
}

context(_: Describer<T>)
fun <T> log(message: T) {}

class A
class B

context(_: Describer<A>, _: Describer<B>)
fun bar() {
    log(A())
}