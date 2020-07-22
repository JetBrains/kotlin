// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    companion object {
        class B
    }
}

class C: A() {
    val b: <!OTHER_ERROR, OTHER_ERROR!>B<!> = null!!

    init {
        <!UNRESOLVED_REFERENCE!>B<!>()
    }

    object O {
        val b: <!OTHER_ERROR, OTHER_ERROR!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    class K {
        val b: <!OTHER_ERROR, OTHER_ERROR!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    inner class I {
        val b: <!OTHER_ERROR, OTHER_ERROR!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }
}
