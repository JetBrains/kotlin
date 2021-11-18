// !LANGUAGE: +ContextReceivers

class A
class B

fun expectAB(f: context(A, B) () -> Unit) {
    f(A(), <!TOO_MANY_ARGUMENTS!>B()<!>)
}

fun test() {
    val l: context(B, A) () -> Unit = { }
    expectAB(l)
}