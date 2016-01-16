fun foo(p: Any, p1: Any?) {
    x(e.f as String)
    y(p as? Int)
    z(f() as String)

    if (a) {
        print((p as String).length)
    }
    else {
        print((p as String).get(1))
    }

    if (y()) {
        print(<caret>p[1])
        p1 as String
    }

    z(p1 as String)
}
