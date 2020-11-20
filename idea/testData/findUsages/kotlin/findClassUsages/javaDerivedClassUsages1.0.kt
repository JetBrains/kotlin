// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: derivedClasses
interface X {

}

open class <caret>A: X {

}

open class C: Y {

}

interface Z: A {

}

// DISABLE-ERRORS