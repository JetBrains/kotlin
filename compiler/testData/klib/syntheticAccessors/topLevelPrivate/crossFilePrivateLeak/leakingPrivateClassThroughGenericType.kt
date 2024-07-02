// IGNORE_BACKEND: ANY

// FILE: a.kt
private class Private

private inline fun <reified T> parametrized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = parametrized<Private>()

// FILE: main.kt
fun box(): String {
    return inlineFun()
}
