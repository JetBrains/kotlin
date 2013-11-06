// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedInterfaces
trait <caret>X {

}

open class A: X {

}

open class C: Y {

}

trait Z: A {

}