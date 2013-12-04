// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedInterfaces
fun foo() {
    open class <caret>A

    trait T: A

    fun bar() {
        trait U: T
    }
}

