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
            <!RETURN_NOT_ALLOWED!>return@A<!>
        else if (2 < 3)
            <!RETURN_NOT_ALLOWED!>return@inner<!>
        return@outer
    }
}
