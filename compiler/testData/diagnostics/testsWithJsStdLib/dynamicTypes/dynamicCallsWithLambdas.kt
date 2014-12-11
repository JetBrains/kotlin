fun test(d: dynamic) {
    d.foo {}

    d.foo { <!UNRESOLVED_REFERENCE!>it<!> }

    d.foo { x -> }

    d.foo { (x: Int) -> "" }

    d.foo { x, y -> "" }

    d.foo { (x: String, y: Int) -> "" }

    d.foo { (x: String, y: Int): Int -> <!TYPE_MISMATCH!>""<!> }

    d.foo { String.(x: String, y: Int): Int -> length() }
}
