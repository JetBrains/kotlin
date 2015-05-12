// PSI_ELEMENT: org.jetbrains.kotlin.psi.JetClass
// OPTIONS: derivedClasses
interface X {

}

open class <caret>A: X {

}

interface Y: X {

}
