fun foo(f: () -> Unit) {}
fun bar(x: Int, f: () -> Unit) {}
fun baz(f: () -> Unit, other: Boolean = true) {}


fun test() {
    foo {}
    foo() {}
    foo({})

    foo(1) {}
    foo(f = {}) {}

    bar(1) {}
    bar(x = 1) {}
    bar(1, {})
    bar(x = 1, f = {})

    bar {}
    bar({})

    baz(other = false, f = {})
    baz({}, false)

    baz {}
    baz() {}
    baz(other = false) {}
}