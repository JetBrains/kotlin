// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers
// DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> listOf(vararg e: T): List<T> = null!!

class A<T>

context(List<T>)
fun <T> A<T>.f() {}

fun main() {
    with(listOf(1, 2, 3)) {
        A<Int>().f()
    }
    with(listOf("1", "2", "3")) {
        A<Int>().<!NO_CONTEXT_ARGUMENT!>f<!>()
    }
}
