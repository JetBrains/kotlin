interface A {
    fun <T> foo() where T : Any, T : Cloneable?
}

interface B : A {
    <!NOTHING_TO_OVERRIDE!>override<!> fun <T> foo() where T : Any?, T : Cloneable
}
