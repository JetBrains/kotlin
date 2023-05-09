// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    class X {
        fun A_X() {}
    }

    class Y {
        fun A_Y() {}
    }

    companion object {
        class X {
            fun A_C_X() {}
        }

        class Z {
            fun A_C_Z() {}
        }
    }

    init {
        X().A_X()
        X().<!UNRESOLVED_REFERENCE!>A_C_X<!>()
    }
}

class Simple: A() {
    init {
        Y().A_Y()
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>Z()<!>.A_C_Z()
    }
}

class B: A() {
    class Y {
        fun B_Y() {}
    }

    class Z {
        fun B_Z() {}
    }

    init {
        X().A_X()
        X().<!UNRESOLVED_REFERENCE!>A_C_X<!>()

        Y().B_Y()
        Y().<!UNRESOLVED_REFERENCE!>A_Y<!>()

        Z().B_Z()
        Z().<!UNRESOLVED_REFERENCE!>A_C_Z<!>()
    }

    companion object {
        init {
            X().A_X()
            X().<!UNRESOLVED_REFERENCE!>A_C_X<!>()

            Y().B_Y()
            Y().<!UNRESOLVED_REFERENCE!>A_Y<!>()

            Z().B_Z()
            Z().<!UNRESOLVED_REFERENCE!>A_C_Z<!>()
        }
    }
}

class C: A() {
    companion object {
        class Y {
            fun C_C_Y() {}
        }

        class Z {
            fun C_C_Z() {}
        }

        init {
            Y().C_C_Y()
            Y().<!UNRESOLVED_REFERENCE!>A_Y<!>()

            Z().C_C_Z()
            Z().<!UNRESOLVED_REFERENCE!>A_C_Z<!>()
        }
    }

    init {
        Y().C_C_Y()
        Y().<!UNRESOLVED_REFERENCE!>A_Y<!>()

        Z().C_C_Z()
        Z().<!UNRESOLVED_REFERENCE!>A_C_Z<!>()
    }
}
