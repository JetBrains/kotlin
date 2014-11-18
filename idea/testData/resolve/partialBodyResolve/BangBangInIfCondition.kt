fun foo(p: Boolean?, p1: Any?) {
    if (p!!) {
        print(p1!!.hashCode())
    }

    <caret>p.hashCode()
}