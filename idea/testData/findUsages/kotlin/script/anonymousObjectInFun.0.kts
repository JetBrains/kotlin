// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
fun foo() {
    val x = <caret>object : Any() {}
}
