// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

package a


enum class C {
    E1, E2, E3 {
        <!LOCAL_OBJECT_NOT_ALLOWED!>object O_O<!>

        fun b() {
            O_O
        }

        <!NESTED_CLASS_NOT_ALLOWED!>class G<!>
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
    C.E3.<!UNRESOLVED_REFERENCE!>O<!>.InO

    C.O
    C.O.InO
    C.A()
    C.<!INNER_CLASS_CONSTRUCTOR_NO_RECEIVER!>B<!>()

    C.E3.<!UNRESOLVED_REFERENCE!>O_O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>G<!>()
}
