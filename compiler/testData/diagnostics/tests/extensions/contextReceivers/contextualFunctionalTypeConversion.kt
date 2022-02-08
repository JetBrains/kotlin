// !LANGUAGE: +ContextReceivers

class A
class B

fun expectAB(f: context(A, B) () -> Unit) {
    f(A(), B())
}

fun test() {
    val l: context(B, A) () -> Unit = { }
    expectAB(<!TYPE_MISMATCH!>l<!>)
}