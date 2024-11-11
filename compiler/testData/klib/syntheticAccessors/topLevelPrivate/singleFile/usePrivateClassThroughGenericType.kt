private class Private

private inline fun <reified T> parameterized(): String {
    if (T::class == Private::class) return "OK"
    return T::class.simpleName ?: "Unknown type"
}

@Suppress("IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_ERROR")
internal inline fun inlineFun() = parameterized<Private>()

fun box(): String {
    return inlineFun()
}
