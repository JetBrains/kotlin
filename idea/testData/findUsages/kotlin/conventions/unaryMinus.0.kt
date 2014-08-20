// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>minus(): A = this
}

fun test() {
    A(1).minus()
    -A(1)
}