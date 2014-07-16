// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages
// FIND_BY_REF

class A(val n: Int) {
    override fun equals(other: Any?): Boolean = other is A && other.n == n
}

fun test() {
    A(0) == A(1)
    A(0) != A(1)
    A(0) equals A(1)
    A(0) <caret>identityEquals A(1)
    A(0) === A(1)
    A(0) !== A(1)
}