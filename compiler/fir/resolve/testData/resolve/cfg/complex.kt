// !DUMP_CFG
interface AutoCloseable {
    fun close()
}

fun Throwable.addSuppressed(other: Throwable) {}

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