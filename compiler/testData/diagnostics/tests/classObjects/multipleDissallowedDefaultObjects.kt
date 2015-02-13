class A {
    inner class I {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object A

        <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object B

        <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object C
    }
}

object O {
    <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object A

    <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object B

    <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object C
}