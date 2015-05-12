// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
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

