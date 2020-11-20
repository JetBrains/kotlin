// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages, skipImports

open data class A(val a: Int)

class B(b: Int): A(b) {
    override fun <caret>copy(b: Int): B = B(b)
}

fun main(a: A) {
    a.copy(1)
    B(0).copy(1)
}

// DISABLE-ERRORS
