class A {
    inner class I {
        companion <!NESTED_CLASS_NOT_ALLOWED("Companion object")!>object A<!>

        <!MANY_COMPANION_OBJECTS!>companion<!> <!NESTED_CLASS_NOT_ALLOWED("Companion object")!>object B<!>

        <!MANY_COMPANION_OBJECTS!>companion<!> <!NESTED_CLASS_NOT_ALLOWED("Companion object")!>object C<!>
    }
}

object O {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object A

    <!MANY_COMPANION_OBJECTS, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object B

    <!MANY_COMPANION_OBJECTS, WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object C
}
