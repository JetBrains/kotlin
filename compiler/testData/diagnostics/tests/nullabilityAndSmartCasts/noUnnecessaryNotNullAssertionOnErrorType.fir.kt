package a

fun foo() {
    <!ARGUMENT_TYPE_MISMATCH!>bar()<!>!!
}

fun bar() = <!UNRESOLVED_REFERENCE!>aa<!>
