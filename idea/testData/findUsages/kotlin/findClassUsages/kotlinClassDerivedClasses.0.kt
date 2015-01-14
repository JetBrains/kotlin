// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
trait X {

}

open class <caret>A: X {

}

trait Y: X {

}
