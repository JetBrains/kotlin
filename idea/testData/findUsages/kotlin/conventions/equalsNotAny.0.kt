// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIND_BY_REF

class A(val n: Int) {
    fun foo(a: A) = a <caret>== this

    override override fun equals(other: Any?): Boolean = TODO()
}

fun test() {
    1 == 2
    A(0) == A(1)
    A(0) != A(1)
    A(0) equals A(1)
    A(0) === A(1)
    A(0) !== A(1)
}
