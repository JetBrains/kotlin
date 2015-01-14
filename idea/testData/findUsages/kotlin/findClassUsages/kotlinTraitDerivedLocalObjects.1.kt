// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    object O1: A() {

    }

    fun bar() {
        object O2: X {

        }
    }
}
