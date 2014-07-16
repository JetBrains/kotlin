// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class B(val n: Int) {
    fun <caret>get(i: Int): B = B(i)
}

fun test() {
    B(1).get(2)
    B(1)[2]
}