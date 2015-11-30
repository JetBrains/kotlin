// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    operator fun <caret>unaryMinus(): A = this
}

fun test() {
    A(1).unaryMinus()
    -A(1)
}
