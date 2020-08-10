// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
fun foo(): String {
    val <caret>bar = ""

    return bar
}

val x = bar

// DISABLE-ERRORS