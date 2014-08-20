// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetParameter
// OPTIONS: usages
// FIND_BY_REF

data class A(val n: Int, val s: String, val o: Any)

fun test() {
    val a = A(1, "2", Any())
    a.n
    a.<caret>component1()
    val (x, y, z) = a
}