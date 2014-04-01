class Klass<T>

fun test(obj: Any): String {
    return <caret>if (obj is String)
        "string"
    else if (obj is Int)
        "int"
    else if (obj is Klass<*>)
        "class"
    else "unknown"
}
