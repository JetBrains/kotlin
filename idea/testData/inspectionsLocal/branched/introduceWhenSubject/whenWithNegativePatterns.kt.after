fun test(obj: Any): String {
    return <caret>when (obj) {
        !is Iterable<*> -> "not iterable"
        !is Collection<*> -> "not collection"
        !is MutableCollection<*> -> "not mutable collection"
        else -> "unknown"
    }
}