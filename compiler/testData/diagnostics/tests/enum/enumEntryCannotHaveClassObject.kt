enum class E {
    FIRST,

    SECOND {
        <!COMPANION_OBJECT_NOT_ALLOWED!>companion<!> object {
            fun foo() = 42
        }
    };
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
