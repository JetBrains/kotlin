// IGNORE_BACKEND: ANY

// FILE: A.kt
private class Private

internal inline fun getPrivateKlass(): String {
    val klass = Private::class
    return klass.simpleName ?: "null"
}

// FILE: main.kt
fun box(): String {
    val result = getPrivateKlass()
    if (result != "Private") return result
    return "OK"
}
