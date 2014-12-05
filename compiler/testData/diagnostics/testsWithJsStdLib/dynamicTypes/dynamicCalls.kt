fun test(d: dynamic) {
    val v1 = d.foo()
    v1.isDynamic() // to check that anything is resolvable

    val v2 = d.foo(1)
    v2.isDynamic() // to check that anything is resolvable

    val v3 = d.foo(1, "")
    v3.isDynamic() // to check that anything is resolvable

    val v4 = d.foo<String>()
    v4.isDynamic() // to check that anything is resolvable

    val v5 = d.foo
    v5.isDynamic() // to check that anything is resolvable

    d.foo = 1
}
