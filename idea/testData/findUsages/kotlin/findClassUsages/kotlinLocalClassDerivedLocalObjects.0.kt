// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses
fun foo() {
    open class <caret>A

    object B: A()

    trait T: A

    fun bar() {
        object C: A()

        object D: T
    }
}

