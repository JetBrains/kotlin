// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>plus(m: Int): A = A(n + m)
    fun plus(a: A): A = this + a.n
}

fun test() {
    A(0) + A(1) + 2
    A(0) plus A(1) plus 2
    A(0).plus(A(1).plus(2))

    var a = A(0)
    a += 1
    a += A(1)

    +A(0)
}