class Foo {
    fun bar() {}
    fun f() = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::bar
}

val f: () -> Unit = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::<!UNRESOLVED_REFERENCE!>foo<!>
