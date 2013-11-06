// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetObjectDeclarationName
// OPTIONS: usages
fun foo(): Any {
    object <caret>Bar

    return Bar
}

val x = Bar
