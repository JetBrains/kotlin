// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
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