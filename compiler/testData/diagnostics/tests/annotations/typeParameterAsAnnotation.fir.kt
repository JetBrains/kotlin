class Foo<T> {
    <!UNRESOLVED_REFERENCE!>@T<!>
    fun foo() = 0
}

class Bar<T : Annotation> {
    <!UNRESOLVED_REFERENCE!>@T<!>
    fun foo() = 0
}
