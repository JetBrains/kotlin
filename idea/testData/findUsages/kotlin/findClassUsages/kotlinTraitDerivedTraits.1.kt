// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedInterfaces
open class B: A() {

}

open class C: Y {

}

interface Z: A {

}

interface U: Z {

}
