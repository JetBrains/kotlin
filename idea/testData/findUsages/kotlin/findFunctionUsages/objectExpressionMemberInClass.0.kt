// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_COMPARISON

class Foo {
    private val localObject = object : Any() {
        fun <caret>f() {
        }
    }

    fun bar() {
        localObject.f()
    }
}