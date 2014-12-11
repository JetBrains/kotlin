fun test(d: dynamic) {
    val a = array(1, 2, 3)

    d.foo(*d)
    d.foo(*a)
    d.foo(1, "2", *a)
    d.foo(1, *a) { }
    d.foo(*a) { "" }
    d.foo(*a, *a)
    d.foo(*a, *a) { "" }
    d.foo(*a, 1, { "" }, *a)
    d.foo(*a, 1)
    d.foo(*a, *a, { "" })
}