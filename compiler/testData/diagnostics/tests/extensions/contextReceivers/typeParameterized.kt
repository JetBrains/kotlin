// !LANGUAGE: +ContextReceivers
// !DIAGNOSTICS: -UNUSED_PARAMETER

class A
class B<X>(val x: X)

context(T)
fun <T> T.f(t: B<T>) {}

fun Int.main(a: A, b: B<String>) {
    a.<!NO_CONTEXT_RECEIVER!>f(<!TYPE_MISMATCH!>b<!>)<!>
}