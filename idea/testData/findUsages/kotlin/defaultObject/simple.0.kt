// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetObjectDeclaration
// OPTIONS: usages
class Foo {
    default <caret>object {
        fun f() {
        }
    }
}