class A {
    inner class I {
        companion object A

        companion <!MANY_COMPANION_OBJECTS!>object B<!>

        companion <!MANY_COMPANION_OBJECTS!>object C<!>
    }
}

object O {
    companion object A

    companion <!MANY_COMPANION_OBJECTS!>object B<!>

    companion <!MANY_COMPANION_OBJECTS!>object C<!>
}
