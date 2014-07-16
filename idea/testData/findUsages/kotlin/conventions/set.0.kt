// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class B(val n: Int) {
    fun <caret>set(i: Int, a: B) {}
}

fun test() {
    var a = B(1)
    a.set(2, B(2))
    a[2] = B(2)
    a[2]
}