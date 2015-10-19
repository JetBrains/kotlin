// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>minus(): A = this
}

fun test() {
    A(1).minus()
    -A(1)
}
