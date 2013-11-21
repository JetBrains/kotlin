// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetObjectDeclaration
// OPTIONS: usages
fun foo(): Any {
    object <caret>Bar

    return Bar
}

val x = Bar
