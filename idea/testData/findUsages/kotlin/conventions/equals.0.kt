// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIND_BY_REF

class A(val n: Int) {
    infix override fun equals(other: Any?): Boolean = other is A && other.n <caret>== n
}

fun test() {
    A(0) == A(1)
    A(0) != A(1)
    A(0) equals A(1)
    A(0) === A(1)
    A(0) !== A(1)
}
