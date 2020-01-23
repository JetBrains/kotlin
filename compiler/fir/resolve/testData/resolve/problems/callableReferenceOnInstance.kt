interface KaptLogger {
    val isVerbose: Boolean

    fun warn(message: String)
    fun error(message: String)
}

fun test(logger: KaptLogger) {
    val func = if (logger.isVerbose)
        <!UNRESOLVED_REFERENCE!>logger::warn<!>
    else
        <!UNRESOLVED_REFERENCE!>logger::error<!>
}