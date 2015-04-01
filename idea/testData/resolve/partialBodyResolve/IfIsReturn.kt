fun foo(p: Any?) {
    if (p is String) return
    println(<caret>p.hashCode())
}