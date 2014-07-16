// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(var n: Int) {
    fun <caret>plusAssign(m: Int) {
        n += m
    }

    fun plusAssign(a: A) {
        this += a.n
    }
}

fun test() {
    val a = A(0)
    a.plusAssign(1)
    a += 1
    a += A(1)
}