// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion

open class A {
    companion object {
        class B
    }
}

class C: A() {
    val b: <!UNRESOLVED_REFERENCE!>B<!> = null!!

    init {
        <!UNRESOLVED_REFERENCE!>B<!>()
    }

    object O {
        val b: <!UNRESOLVED_REFERENCE!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    class K {
        val b: <!UNRESOLVED_REFERENCE!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }

    inner class I {
        val b: <!UNRESOLVED_REFERENCE!>B<!> = null!!

        init {
            <!UNRESOLVED_REFERENCE!>B<!>()
        }
    }
}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, companionObject, init, inner, nestedClass, objectDeclaration,
propertyDeclaration */
