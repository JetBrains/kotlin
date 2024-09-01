// IGNORE_BACKEND: ANY
// ^^^ Muted because a private type is leaked from the declaring file, and the visibility validator detects this.
//     This test should be converted to a test that checks reporting private types exposure. To be done in KT-69681.

// MODULE: lib
// FILE: a.kt
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

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val result = isPrivate(Any()) + asPrivate(Any())
    if (result != "OK1OK2") return result
    return "OK"
}
