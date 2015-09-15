fun foo(p: Any?) {
    if (p !is String) {
        if (p == null) {
            print("null")
            return
        }
        else {
            return
        }
    }
    println(<caret>p.length())
}