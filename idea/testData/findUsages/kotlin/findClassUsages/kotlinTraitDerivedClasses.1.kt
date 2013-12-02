// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedClasses
open class B: A() {

}

open class C: Y {

}

trait Z: A {

}

trait U: Z {

}