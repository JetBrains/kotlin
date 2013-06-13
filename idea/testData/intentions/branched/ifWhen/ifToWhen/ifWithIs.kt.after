fun test(obj: Any): String {
    return <caret>when (obj) {
        is String -> "string"
        is Int -> "int"
        is Class<*> -> "class"
        else -> "unknown"
    }
}