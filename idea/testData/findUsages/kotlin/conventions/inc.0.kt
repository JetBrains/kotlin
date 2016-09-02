// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    operator fun <caret>inc(): A = A(n + 1)
}

fun test() {
    var a = A(1)
    a.inc()
    ++a
    a++
}
