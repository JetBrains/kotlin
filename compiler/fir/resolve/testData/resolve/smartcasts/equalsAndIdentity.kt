interface A {
    fun foo()
}

fun test_1(x: A, y: A?) {
    if (x == y) {
        x.foo()
        y.foo()
    }
    if (x === y) {
        x.foo()
        y.foo()
    }
}

fun test_2(x: A?, y: A?) {
    if (x == y) {
        x.foo()
        y.foo()
    }
    if (x === y) {
        x.foo()
        y.foo()
    }
}

fun test_3(x: A?, y: A?) {
    if (y == null) return
    if (x == y) {
        x.foo()
        y.foo()
    }
    if (x === y) {
        x.foo()
        y.foo()
    }
}