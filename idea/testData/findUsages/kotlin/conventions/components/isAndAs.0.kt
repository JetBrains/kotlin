// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>x: Int, val y: Int)

fun x(o: Any) {
    if (o is A) {
        val (x, y) = o
        val (x1, y1) = A(1, "", "")
    }
}

fun y(o: Any) {
    val list = o as List<A>
    val (x, y) = list[0]
}
// DISABLE-ERRORS