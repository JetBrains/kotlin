// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
class Foo {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun <caret>foo(n: Int = 1) {

        }
    }
}