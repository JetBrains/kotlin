class A
class B
class C

context(A)
fun B.f() {}

fun main() {
    val b = B()

    b.<!NO_CONTEXT_RECEIVER!>f()<!>
    with(A()) {
        b.f()
    }
    with(C()) {
        b.<!NO_CONTEXT_RECEIVER!>f()<!>
    }
}