fun test(obj: Any): String {
    return <caret>if (obj !is Iterable<*>)
        "not iterable"
    else if (obj !is Collection<*>)
        "not collection"
    else if (obj !is MutableCollection<*>)
        "not mutable collection"
    else "unknown"
}