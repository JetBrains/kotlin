fun test(value: Any): String {
    return when (val <caret>v = value) {
        is String -> "$v is String"
        is Int -> "$v is Int"
        else -> "$v"
    }
}