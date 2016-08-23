// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages

open class X {
    operator fun <caret>component1(): Int = 0
    operator fun component2(): Int = 1
}

open class Y : X()
open class Z : Y()

fun f() = X()
fun g() = Y()
fun h() = Z()

fun test() {
    val (x, y) = f()
    val (x1, y1) = g()
    val (x2, y2) = h()
}
