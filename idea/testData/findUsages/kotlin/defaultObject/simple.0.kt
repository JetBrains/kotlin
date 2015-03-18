// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetObjectDeclaration
// OPTIONS: usages
class Foo {
    companion <caret>object {
        fun f() {
        }
    }
}