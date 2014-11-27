fun foo(o: Any?, p: Int) {
    if (p > 0) {
        if (o == null) return
    }
    else {
        if (o !is String) return
    }
    <caret>o.javaClass
}