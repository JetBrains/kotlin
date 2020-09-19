// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_IGNORE

class Foo {
    companion object {
        private val localObject = object : Any() {
            fun <caret>f() {
            }
        }
    }

    fun bar() {
        localObject.f()
    }
}