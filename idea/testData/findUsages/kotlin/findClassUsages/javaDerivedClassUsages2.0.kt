// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
interface <caret>X {

}

open class A: X {

}

open class C: Y {

}

interface Z: A {

}
