// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages
package pack

data class A(val <caret>n: Int, val s: String, val o: Any)

fun A.ext1() {
    val (x, y) = getThis()
}

fun List<A>.ext1() {
    val (x, y) = get(0)
}

fun <T> T.getThis(): T = this