// FIR_COMPARISON
// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedInterfaces
interface <caret>X {

}

open class A: X {

}

interface Y: X {

}
