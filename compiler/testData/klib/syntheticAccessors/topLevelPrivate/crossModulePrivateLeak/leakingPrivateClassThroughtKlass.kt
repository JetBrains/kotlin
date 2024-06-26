// IGNORE_BACKEND: ANY

// MODULE: lib
// FILE: a.kt
private class Private

internal inline fun getPrivateKlass(): String {
    val klass = Private::class
    return klass.simpleName ?: "null"
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String {
    val result = getPrivateKlass()
    if (result != "Private") return result
    return "OK"
}
