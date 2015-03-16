class A {
    inner class I {
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object A

        <!MANY_COMPANION_OBJECTS, COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object B

        <!MANY_COMPANION_OBJECTS, COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object C
    }
}

object O {
    <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object A

    <!MANY_COMPANION_OBJECTS, COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object B

    <!MANY_COMPANION_OBJECTS, COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object C
}