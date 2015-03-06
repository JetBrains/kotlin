enum class E {
    FIRST

    SECOND {
        default <!DEFAULT_OBJECT_NOT_ALLOWED!>object<!> {
            fun foo() = 42
        }
    }
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
