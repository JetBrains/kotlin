// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetParameter
// OPTIONS: usages

data class A(val <caret>n: Int, val s: String, val o: Any)

fun test() {
    val a = A(1, "2", Any())
    a.n
    a.component1()
    val (x, y, z) = a
}