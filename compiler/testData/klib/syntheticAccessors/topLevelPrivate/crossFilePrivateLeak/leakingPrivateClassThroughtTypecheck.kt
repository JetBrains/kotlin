// IGNORE_BACKEND: ANY

// FILE: A.kt
private class Private

internal inline fun isPrivate(obj: Any): String = when (obj) {
    is Private -> "isPrivate"
    else -> "OK1"
}

internal inline fun asPrivate(obj: Any): String {
    try {
        val privateObj = obj as Private
        return "asPrivate"
    } catch (e: ClassCastException) {
        return "OK2"
    }
}

// FILE: main.kt
fun box(): String {
    val result = isPrivate(Any()) + asPrivate(Any())
    if (result != "OK1OK2") return result
    return "OK"
}
