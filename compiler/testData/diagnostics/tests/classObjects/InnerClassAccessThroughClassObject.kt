package a

class A {
    class Nested
    inner class Inner


    default object {

        class Nested2

        val c: Int = 1

        object Obj2 {
            val c: Int = 1
        }
    }

    object Obj
}

object O {
    class A

    object O
}

fun f() {
    A.c
    A.hashCode()
    A().<!NO_DEFAULT_OBJECT, NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE!>Nested<!>
    A.Nested()
    A().Inner()
    A.Default.<!UNRESOLVED_REFERENCE!>Nested<!>
    A.Default.<!UNRESOLVED_REFERENCE!>Inner<!>
    A.<!UNRESOLVED_REFERENCE!>Inner<!>
    A.Default.c
    A.Default.Obj2
    A.Default.Obj2.c

    A.Default.Nested2()
    A.Default.c
    A.Obj
    A.Default.Obj2
    A.<!UNRESOLVED_REFERENCE!>Obj2<!>
    A.<!UNRESOLVED_REFERENCE!>Obj2<!>.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>c<!>
    A.<!UNRESOLVED_REFERENCE!>Nested2<!>

    O.O
    O.A()
}