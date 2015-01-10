// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetNamedFunction
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
