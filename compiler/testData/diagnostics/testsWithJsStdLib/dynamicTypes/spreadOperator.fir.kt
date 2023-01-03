fun test(d: dynamic) {
    val a = arrayOf(1, 2, 3)

    d.foo(*d)
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    d.foo(1, "2", *<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    d.foo(1, *<!ARGUMENT_TYPE_MISMATCH!>a<!>) <!VARARG_OUTSIDE_PARENTHESES!>{ }<!>
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>, *<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>, *<!ARGUMENT_TYPE_MISMATCH!>a<!>) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>, 1, { "" }, *<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>, 1)
    d.foo(*<!ARGUMENT_TYPE_MISMATCH!>a<!>, *<!ARGUMENT_TYPE_MISMATCH!>a<!>, { "" })

    bar(d)
    bar(d, d)
    bar(*d)
    bar(*d, *d)
    bar(*d, 23, *d)
}

fun bar(vararg x: Int): Unit = TODO("$x")
