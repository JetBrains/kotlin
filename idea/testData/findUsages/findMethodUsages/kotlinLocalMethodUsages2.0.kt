// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages
fun foo() {
    if (true) {
        fun <caret>bar() {

        }

        bar()
    }

    bar()
}

bar()
