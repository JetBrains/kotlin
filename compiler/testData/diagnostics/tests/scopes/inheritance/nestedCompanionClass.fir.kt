// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    companion object {
        class B
    }
}

class C: A() {
    val b: B = null!!

    init {
        <!UNRESOLVED_REFERENCE!>B<!>()
    }

    object O {
        val b: B = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    class K {
        val b: B = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    inner class I {
        val b: B = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }
}
