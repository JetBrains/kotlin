// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

package a


enum class C {
    E1, E2, E3 {
        <!NESTED_CLASS_DEPRECATED!>object O_O<!>

        fun b() {
            O_O
        }

        <!NESTED_CLASS_DEPRECATED!>class G<!>
    },

    E4 {
        fun c() {
            this.B()

            C.A()
            A()
            //TODO: should be resolved with error
            this.<!UNRESOLVED_REFERENCE!>A<!>()
        }
    };

    class A
    inner class B
    object O {
        object InO
    }
}

fun f() {
    C.E1.<!UNRESOLVED_REFERENCE!>A<!>
    C.E1.<!UNRESOLVED_REFERENCE!>A<!>()
    C.E2.B()

    C.E2.<!UNRESOLVED_REFERENCE!>O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>O<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>InO<!>

    C.O
    C.O.InO
    C.A()
    C.<!RESOLUTION_TO_CLASSIFIER!>B<!>()

    C.E3.<!UNRESOLVED_REFERENCE!>O_O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>G<!>()
}
