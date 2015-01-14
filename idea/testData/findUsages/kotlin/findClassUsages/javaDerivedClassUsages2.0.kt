// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
trait <caret>X {

}

open class A: X {

}

open class C: Y {

}

trait Z: A {

}
