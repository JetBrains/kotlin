fun test(obj: Any): String {
    return <caret>if (obj is String)
        "string"
    else if (obj is Int)
        "int"
    else if (obj is Class<*>)
        "class"
    else "unknown"
}