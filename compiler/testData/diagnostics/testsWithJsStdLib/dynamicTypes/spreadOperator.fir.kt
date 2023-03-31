fun test(d: dynamic) {
    val a = arrayOf(1, 2, 3)

    d.foo(*d)
    d.foo(*a)
    d.foo(1, "2", *a)
    d.foo(1, *a) <!VARARG_OUTSIDE_PARENTHESES!>{ }<!>
    d.foo(*a) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(*a, *a)
    d.foo(*a, *a) <!VARARG_OUTSIDE_PARENTHESES!>{ "" }<!>
    d.foo(*a, 1, { "" }, *a)
    d.foo(*a, 1)
    d.foo(*a, *a, { "" })

    bar(d)
    bar(d, d)
    bar(*d)
    bar(*d, *d)
    bar(*d, 23, *d)
}

fun bar(vararg x: Int): Unit = TODO("$x")
