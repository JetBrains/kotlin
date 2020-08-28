// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: usages, constructorUsages
fun foo(): Any {
    if (false) {
        class <caret>Bar

        return Bar()
    }

    return Bar()
}

val x = Bar()

// DISABLE-ERRORS