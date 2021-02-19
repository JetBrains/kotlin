class A {
    inner class I {
        companion <!NESTED_CLASS_NOT_ALLOWED!>object A<!>

        companion <!MANY_COMPANION_OBJECTS, NESTED_CLASS_NOT_ALLOWED!>object B<!>

        companion <!MANY_COMPANION_OBJECTS, NESTED_CLASS_NOT_ALLOWED!>object C<!>
    }
}

object O {
    companion object A

    companion <!MANY_COMPANION_OBJECTS!>object B<!>

    companion <!MANY_COMPANION_OBJECTS!>object C<!>
}
