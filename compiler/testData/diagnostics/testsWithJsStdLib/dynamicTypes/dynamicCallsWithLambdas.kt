fun test(d: dynamic) {
    d.foo {}

    d.foo { <!UNRESOLVED_REFERENCE!>it<!> }

    d.foo { x -> }

    d.foo { <!DEPRECATED_LAMBDA_SYNTAX!>(x: Int)<!> -> "" }

    d.foo { x, y -> "" }

    d.foo { <!DEPRECATED_LAMBDA_SYNTAX!>(x: String, y: Int)<!> -> "" }

    d.foo { <!DEPRECATED_LAMBDA_SYNTAX!>(x, y: Int)<!> -> "" }

    d.foo { <!DEPRECATED_LAMBDA_SYNTAX!>(x: String, y: Int): Int<!> -> <!TYPE_MISMATCH!>""<!> }

    d.foo { <!DEPRECATED_LAMBDA_SYNTAX!>String.(x: String, y: Int): Int<!> -> length() }

    d.foo({})

    d.foo({ x -> })

    d.foo({ x -> } : (Int) -> Unit)

    d.foo(label@ { x -> })

    d.foo(label@ ({ x, y -> }))

    d.foo((label@ ({ <!DEPRECATED_LAMBDA_SYNTAX!>(x, y: Int)<!> -> })))

    d.foo(({ x -> }))

    d.foo((({ x -> })))
}
