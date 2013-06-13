fun test(obj: Any): String {
    return <caret>when {
        obj !is Iterable<*> -> "not iterable"
        obj !is Collection<*> -> "not collection"
        obj !is MutableCollection<*> -> "not mutable collection"
        else -> "unknown"
    }
}