fun foo(p: Any) {
    if (p !is String) {
        kotlin.error("Not String")
    }
    println(<caret>p.length())
}