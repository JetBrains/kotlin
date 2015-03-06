package a


enum class C {
    E1 E2 E3 {
        object O_O

        fun b() {
            O_O
        }

        class G
    }

    E4 {
        fun c() {
            //TODO: this is a bug
            this.<!UNRESOLVED_REFERENCE!>B<!>()

            C.A()
            A()
            //TODO: this is a bug
            this.<!UNRESOLVED_REFERENCE!>A<!>()
        }
    }

    class A
    inner class B
    object O {
        object InO
    }
}

fun f() {
    C.E1.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE, FUNCTION_CALL_EXPECTED!>A<!>
    C.E1.<!NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>A<!>()
    C.E2.B()

    C.E2.<!UNRESOLVED_REFERENCE!>O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>O<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>InO<!>

    C.O
    C.O.InO
    C.A()
    C.<!UNRESOLVED_REFERENCE!>B<!>()

    C.E3.<!UNRESOLVED_REFERENCE!>O_O<!>
    C.E3.<!UNRESOLVED_REFERENCE!>G<!>()
}