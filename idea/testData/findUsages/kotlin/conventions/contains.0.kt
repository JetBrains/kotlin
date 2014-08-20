// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>contains(k: Int): Boolean = k <= n
}

fun test() {
    A(2) contains 1
    1 in A(2)
    1 !in A(2)
    when (1) {
        in A(2) -> {}
        !in A(2) -> {}
    }
}