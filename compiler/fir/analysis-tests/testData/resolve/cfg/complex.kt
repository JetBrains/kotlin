// IGNORE_REVERSED_RESOLVE
// !DUMP_CFG
interface AutoCloseable {
    fun close()
}

internal fun AutoCloseable?.closeFinally(cause: Throwable?) = when {
    this == null -> {}
    cause == null -> close()
    else ->
        try {
            close()
        } catch (closeException: Throwable) {
            cause.addSuppressed(closeException)
        }
}

inline fun <reified T : Any> List<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}
