fun foo(o: String?, o1: String) {
    if (o != o1) return
    <caret>o.hashCode()
}