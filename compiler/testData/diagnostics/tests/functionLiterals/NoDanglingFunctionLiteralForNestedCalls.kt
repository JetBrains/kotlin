package baz

fun test() {
    <!NONE_APPLICABLE!>foo<!>(1) <!DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED!>{}<!>

    foo( <!NONE_APPLICABLE!>foo<!>(1) {} ) //here
}

fun foo(<!UNUSED_PARAMETER!>i<!>: Int) {}

fun foo() : (i : () -> Unit) -> Unit = {}
