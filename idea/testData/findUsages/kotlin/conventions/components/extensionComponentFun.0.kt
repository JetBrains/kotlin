// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtFunction
// OPTIONS: usages

open class X

class Y : X()

operator fun X.component1(): Int = 0
operator fun X.<caret>component2(): Int = 1


fun f() = X()

fun test() {
    val (x, y) = f()
}

fun Y.ext() {
    val (a, b) = this
}
