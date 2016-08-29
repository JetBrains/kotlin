// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>b: B, val n: Int)
data class B(val a: A?, val s: String)

fun f(a: A) {
    val (b, n) = a
    val (a1, s) = b
}