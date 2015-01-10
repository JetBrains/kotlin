// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
trait Z: A {

}

object O1: A()

object O2: Z
