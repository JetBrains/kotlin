// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
// OPTIONS: usages
fun foo() {
    fun <caret>bar() {

    }

    bar()
}

bar()
