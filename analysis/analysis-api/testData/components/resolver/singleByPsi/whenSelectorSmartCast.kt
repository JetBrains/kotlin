fun test(obj: Any): String {
    return when {
        obj !is Iterable<*> -> "not iterable"
        <expr>obj</expr> !is Collection<*> -> "not collection"
        else -> "unknown"
    }
}