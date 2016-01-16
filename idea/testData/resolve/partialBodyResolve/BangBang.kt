fun foo(p: String?, p1: Any?) {
    x(e.f!!)
    y(f()!!)

    if (a) {
        print(p!!.length)
    }
    else {
        print(p!!.get(1))
    }

    if (y()) {
        print(<caret>p[1])
        p1!!
    }

    z(p1!!)
}
