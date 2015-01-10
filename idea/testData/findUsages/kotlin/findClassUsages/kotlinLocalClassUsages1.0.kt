// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: usages, constructorUsages
fun foo(): Any {
    class <caret>Bar

    return Bar()
}

val x = Bar()
