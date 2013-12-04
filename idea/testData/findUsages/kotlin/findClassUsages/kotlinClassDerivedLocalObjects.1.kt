// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses

fun foo() {
    trait Z: A {

    }

    object O1: A()

    fun bar() {
        object O2: Z
    }
}