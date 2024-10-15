// RUN_PIPELINE_TILL: SOURCE
class Foo {
    fun bar() {}
    fun f() = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::<!DEBUG_INFO_MISSING_UNRESOLVED!>bar<!>
}

val f: () -> Unit = <!UNRESOLVED_REFERENCE!>Unresolved<!>()::<!DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>
