// !LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    companion object {
        class B
    }
}

class C: A() {
    val b: <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B<!> = null!!

    init {
        <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B()<!>
    }

    object O {
        val b: <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B<!> = null!!

        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B()<!>
        }
    }

    class K {
        val b: <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B<!> = null!!

        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B()<!>
        }
    }

    inner class I {
        val b: <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B<!> = null!!

        init {
            <!DEPRECATED_ACCESS_BY_SHORT_NAME!>B()<!>
        }
    }
}
