// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtParameter
// OPTIONS: usages

data class A(val <caret>x: Int, val y: Int) {
    companion object {
        fun b(): B = B()
    }
}

data class B(val x: Int, val y: Int)

fun foo() {
    val (x, y) = A.b()
}

// DISABLE-ERRORS