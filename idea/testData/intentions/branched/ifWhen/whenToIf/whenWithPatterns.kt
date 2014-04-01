class Klass<T>

fun test(obj: Any): String {
    return <caret>when (obj) {
        is String -> "string"
        is Int -> "int"
        is Klass<*> -> "class"
        else -> "unknown"
    }
}
