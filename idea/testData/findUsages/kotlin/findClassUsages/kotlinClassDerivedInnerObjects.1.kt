// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
class Outer {
    interface Z: A {

    }

    object O1: A()

    class Inner {
        object O2: Z
    }
}
