private class Private

private inline fun <reified T> parametrized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

internal inline fun inlineFun() = parametrized<Private>()

fun box(): String {
    return inlineFun()
}
