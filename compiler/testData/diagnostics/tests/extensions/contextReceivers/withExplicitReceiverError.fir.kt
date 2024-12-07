// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

class A
class B
class C

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.<!NO_CONTEXT_ARGUMENT!>f<!>()
    with(A()) {
        b.f()
    }
    with(C()) {
        b.<!NO_CONTEXT_ARGUMENT!>f<!>()
    }
}
