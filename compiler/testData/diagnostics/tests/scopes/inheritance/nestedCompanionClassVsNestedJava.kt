// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

// FILE: 1.kt
open class A {
    class Y {
        fun A_Y() {}
    }

    companion object {
        class Z {
            fun A_C_Z() {}
        }
    }
}

// FILE: B.java
public class B extends A {
    class Y {
        void B_Y() {}
    }

    class Z {
        void B_Z() {}
    }
}

// FILE: C.java
public class C extends A {}

// FILE: 2.kt
class E: B() {
    init {
        Y().B_Y()
        Y().<!UNRESOLVED_REFERENCE!>A_Y<!>()

        Z().B_Z()
        Z().<!UNRESOLVED_REFERENCE!>A_C_Z<!>()
    }
}

class Y: C() {
    init {
        Y().A_Y()

        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>Z()<!>.A_C_Z()
    }
}
