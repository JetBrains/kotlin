// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    override fun <caret>equals(other: Any?): Boolean = other is A && other.n == n
}

fun test() {
    A(0) == A(1)
    A(0) != A(1)
    A(0) equals A(1)
    A(0) === A(1)
    A(0) !== A(1)
}