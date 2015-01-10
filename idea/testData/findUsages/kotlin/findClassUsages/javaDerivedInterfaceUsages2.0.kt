// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedInterfaces
trait <caret>X {

}

open class A: X {

}

open class C: Y {

}

trait Z: A {

}
