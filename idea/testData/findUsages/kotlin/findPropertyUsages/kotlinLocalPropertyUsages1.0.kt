// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetProperty
// OPTIONS: usages
fun foo(): String {
    val <caret>bar = ""

    return bar
}

val x = bar
