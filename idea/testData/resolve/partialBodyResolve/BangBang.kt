fun foo(p: String?, p1: Any?) {
    x(e.f!!)
    y(f()!!)

    if (a) {
        print(p!!.size)
    }
    else {
        print(p!!.length)
    }

    if (y()) {
        print(<caret>p.size)
        p1!!
    }

    z(p1!!)
}
