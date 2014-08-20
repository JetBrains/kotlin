// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A(val n: Int) {
    fun <caret>compareTo(other: A): Int = compareTo(other.n)
    fun compareTo(m: Int): Int = n.compareTo(m)
}

fun test() {
    A(0) compareTo A(1)
    A(0) < A(1)
    A(0) <= A(1)
    A(0) > A(1)
    A(0) >= A(1)
    A(0) compareTo 1
    A(0) < 1
    A(0) <= 1
    A(0) > 1
    A(0) >= 1
}