fun test(d: dynamic) {
    d.foo(name = "name")

    d.foo(1, name = "name")

    d.foo(1, duplicate = "", duplicate = "")
}
