enum class E {
    FIRST

    SECOND {
        <!CLASS_OBJECT_NOT_ALLOWED!>class object {
            fun foo() = 42
        }<!>
    }
}

fun f() = E.SECOND.<!UNRESOLVED_REFERENCE!>foo<!>()
