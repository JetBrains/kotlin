// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>inc(): A = A(n + 1)
}

fun test() {
    var a = A(1)
    a.inc()
    ++a
    a++
}