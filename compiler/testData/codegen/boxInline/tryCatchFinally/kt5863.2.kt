inline fun <R> performWithFinally(finally: () -> R) : R {
    try {
        throw RuntimeException("1")
    } catch (e: RuntimeException) {
        throw RuntimeException("2")
    } finally {
        return finally()
    }
}
