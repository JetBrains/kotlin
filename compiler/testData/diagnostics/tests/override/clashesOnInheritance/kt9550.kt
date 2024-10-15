// RUN_PIPELINE_TILL: FRONTEND
interface A {
    fun <T> foo()
    fun <T> bar()
}

interface B {
    fun foo()
    fun bar()
}

<!CONFLICTING_INHERITED_MEMBERS!>interface C1<!> : A, B {
    <!CONFLICTING_OVERLOADS!>override fun bar()<!>
}
