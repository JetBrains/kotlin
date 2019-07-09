// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
fun foo() {
    class <caret>Local

    val x = Local()
}
