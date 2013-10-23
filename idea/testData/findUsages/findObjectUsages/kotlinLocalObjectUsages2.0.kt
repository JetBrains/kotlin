// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetObjectDeclarationName
// OPTIONS: usages
fun foo(): Any {
    if (true) {
        object <caret>Bar

        return Bar
    }

    return Bar
}

val x = Bar
