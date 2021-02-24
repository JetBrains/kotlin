// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
fun foo() {
    open class <caret>A

    object B: A()

    interface T: A

    fun bar() {
        object C: A()

        object D: T
    }
}

// DISABLE-ERRORS