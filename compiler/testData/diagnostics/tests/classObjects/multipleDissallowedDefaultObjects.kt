class A {
    inner class I {
        default <!DEFAULT_OBJECT_NOT_ALLOWED!>object A<!>

        default <!MANY_DEFAULT_OBJECTS, MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>object B<!>

        default <!MANY_DEFAULT_OBJECTS, MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>object C<!>
    }
}

object O {
    default <!DEFAULT_OBJECT_NOT_ALLOWED!>object A<!>

    default <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>object B<!>

    default <!MANY_DEFAULT_OBJECTS, DEFAULT_OBJECT_NOT_ALLOWED!>object C<!>
}