// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
class Foo {
    companion object {
        @JvmStatic fun <caret>foo() {

        }
    }
}