// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedInterfaces
fun foo() {
    open class <caret>A

    interface T: A

    fun bar() {
        interface U: T
    }
}

