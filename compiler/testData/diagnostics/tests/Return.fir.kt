package <!SYNTAX!>return<!>

class A {
    fun outer() {
        fun inner() {
            if (1 < 2)
                return@inner
            else
            return@outer
        }
        if (1 < 2)
            return@A
        else if (2 < 3)
            return@inner
        return@outer
    }
}
