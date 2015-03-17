fun foo(p: Any?, p1: Any?) {
    if (x()) {
        y(p!!)
    }
    else {
        z(p1!!)
    }

    <caret>xxx
}