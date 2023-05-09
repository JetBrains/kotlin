// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// !CHECK_TYPE

fun test(d: dynamic) {
    d.foo {}

    d.foo { <!UNRESOLVED_REFERENCE!>it<!> }

    d.foo { x -> x.bar() }

    d.foo { x: Int -> "" }

    d.foo { x, y -> "" }

    d.foo { x: String, y: Int -> "" }

    d.foo { x, y: Int -> "" }

    d.foo({})

    d.foo({ x -> })

    d.foo(<!UNRESOLVED_REFERENCE!>checkSubtype<!><(Int) -> Unit>({ x -> }))

    d.foo(label@ { x -> })

    d.foo(label@ ({ x, y -> }))

    d.foo((label@ ({ x, y: Int -> })))

    d.foo(({ x -> }))

    d.foo((({ x -> })))
}
