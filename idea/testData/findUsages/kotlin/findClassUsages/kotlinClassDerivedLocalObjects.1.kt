// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    interface Z: A {

    }

    object O1: A()

    fun bar() {
        object O2: Z
    }
}
