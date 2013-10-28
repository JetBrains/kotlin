// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetClass
// OPTIONS: derivedInterfaces
trait X {

}

open class <caret>A: X {

}

trait Y: X {

}