class A {
    inner class I {
        companion <!NESTED_CLASS_NOT_ALLOWED!>object A<!>

        <!MANY_COMPANION_OBJECTS!>companion<!> <!NESTED_CLASS_NOT_ALLOWED!>object B<!>

        <!MANY_COMPANION_OBJECTS!>companion<!> <!NESTED_CLASS_NOT_ALLOWED!>object C<!>
    }
}

object O {
    companion object A

    <!MANY_COMPANION_OBJECTS!>companion<!> object B

    <!MANY_COMPANION_OBJECTS!>companion<!> object C
}
