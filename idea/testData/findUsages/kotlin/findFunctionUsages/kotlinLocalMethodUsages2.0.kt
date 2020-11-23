// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages
// FIR_COMPARISON

fun foo() {
    if (true) {
        fun <caret>bar() {

        }

        bar()
    }

    bar()
}

bar()

// DISABLE-ERRORS