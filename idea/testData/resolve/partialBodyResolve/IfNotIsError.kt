fun foo(p: Any) {
    if (p !is String) {
        error("Not String")
    }
    println(<caret>p.length())
}