enum class E {
    FIRST,

    SECOND {
        companion object {
            fun foo() = 42
        }
    };
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
