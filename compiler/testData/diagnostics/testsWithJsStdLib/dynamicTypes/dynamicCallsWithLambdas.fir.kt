// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// !CHECK_TYPE

fun test(d: dynamic) {
    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{}<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ <!UNRESOLVED_REFERENCE!>it<!> }<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ x -> }<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ x: Int -> "" }<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ x, y -> "" }<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ x: String, y: Int -> "" }<!>

    d.foo <!VARARG_OUTSIDE_PARENTHESES!>{ x, y: Int -> "" }<!>

    d.foo({})

    d.foo({ x -> })

    d.foo(<!UNRESOLVED_REFERENCE!>checkSubtype<!><(Int) -> Unit>({ x -> }))

    d.foo(label@ { x -> })

    d.foo(label@ ({ x, y -> }))

    d.foo((label@ ({ x, y: Int -> })))

    d.foo(({ x -> }))

    d.foo((({ x -> })))
}
