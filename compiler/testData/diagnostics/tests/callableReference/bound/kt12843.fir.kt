class Foo {
    fun bar() {}
    fun f() = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::<!UNRESOLVED_REFERENCE!>bar<!>
}

val f: () -> Unit = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::<!UNRESOLVED_REFERENCE!>foo<!>
