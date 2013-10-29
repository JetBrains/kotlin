// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: usages, constructorUsages
fun foo(): Any {
    if (false) {
        class <caret>Bar

        return Bar()
    }

    return Bar()
}

val x = Bar()
