// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
interface <caret>X {

}

open class A: X {

}

open class C: Y {

}

interface Z: A {

}

// DISABLE-ERRORS