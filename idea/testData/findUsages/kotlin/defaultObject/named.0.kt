// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetObjectDeclaration
// OPTIONS: usages
class Foo {
    default object <caret>Bar {
        fun f() {
        }
    }
}