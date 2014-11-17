fun foo(p: Any, p1: Any?) {
    x(e.f as String)
    y(p as? Int)
    z(f() as String)

    if (a) {
        print((p as String).size)
    }
    else {
        print((p as String).length)
    }

    if (y()) {
        print(<caret>p.size)
        p1 as String
    }

    z(p1 as String)
}
