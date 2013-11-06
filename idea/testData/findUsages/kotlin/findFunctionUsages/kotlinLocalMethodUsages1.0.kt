// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages
fun foo() {
    fun <caret>bar() {

    }

    bar()
}

bar()
