fun foo(p: Any?) {
    do {
        print(p!!)
    } while (x())

    <caret>p.hashCode()
}