// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers

open class A
class B
class C: A()

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.<!NO_CONTEXT_ARGUMENT!>f<!>()
    with(A()) {
        b.f()
    }
    with(C()) {
        b.f()
    }
}
