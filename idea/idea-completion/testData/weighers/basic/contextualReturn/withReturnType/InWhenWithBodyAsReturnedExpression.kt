fun reportError(): Nothing

fun usage(a: Int?): Int {
    return when {
        a == null -> re<caret>
        else -> a
    }
}

// ORDER: reportError
// ORDER: return
