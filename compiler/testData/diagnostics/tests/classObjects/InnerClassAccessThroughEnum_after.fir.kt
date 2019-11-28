// !LANGUAGE: +NestedClassesInEnumEntryShouldBeInner

package a


enum class C {
    E1, E2, E3 {
        object O_O

        fun b() {
            O_O
        }

        class G
    },

    E4 {
        fun c() {
            this.B()

            C.A()
            A()
            //TODO: should be resolved with error
            this.A()
        }
    };

    class A
    inner class B
    object O {
        object InO
    }
}

fun f() {
    C.E1.A
    C.E1.A()
    C.E2.B()

    C.E2.O
    C.E3.O.InO

    C.O
    C.O.InO
    C.A()
    C.<!UNRESOLVED_REFERENCE!>B<!>()

    C.E3.<!UNRESOLVED_REFERENCE!>O_O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>G<!>()
}
