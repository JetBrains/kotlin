// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    object O1: A() {

    }

    fun bar() {
        object O2: X {

        }
    }
}