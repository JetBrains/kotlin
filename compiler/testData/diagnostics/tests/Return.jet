package <!SYNTAX!>return<!>

class A {
    fun outer() {
        fun inner() {
            if (1 < 2)
                return@inner
            else
                <!RETURN_NOT_ALLOWED!>return@outer<!>
        }
        if (1 < 2)
            <!NOT_A_RETURN_LABEL!>return@A<!>
        else if (2 < 3)
            return<!UNRESOLVED_REFERENCE!>@inner<!>
        return@outer
    }
}
