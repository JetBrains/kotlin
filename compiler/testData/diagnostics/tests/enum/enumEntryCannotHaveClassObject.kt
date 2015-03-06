enum class E {
    FIRST

    SECOND {
        <!DEFAULT_OBJECT_NOT_ALLOWED!>default<!> object {
            fun foo() = 42
        }
    }
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
