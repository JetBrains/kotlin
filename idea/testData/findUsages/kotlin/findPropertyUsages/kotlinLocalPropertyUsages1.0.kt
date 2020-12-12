// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
// FIR_COMPARISON

fun foo(): String {
    val <caret>bar = ""

    return bar
}

val x = bar

// DISABLE-ERRORS