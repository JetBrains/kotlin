enum class E {
    FIRST,

    SECOND {
        <!WRONG_MODIFIER_CONTAINING_DECLARATION!>companion<!> object {
            fun foo() = 42
        }
    };
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
