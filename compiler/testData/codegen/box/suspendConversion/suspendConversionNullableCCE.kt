private fun processDirect(func: (suspend () -> Unit)?) {
}

private fun processNullUnwrapping(runnable: Sam? = null) {
    processDirect(if (runnable == null) null else runnable::run)
}

private fun processDirectNN(func: suspend () -> Unit) {
}

fun interface Sam {
    fun run()
}

fun box(): String {
    val callback = Sam { }
    processNullUnwrapping(callback)
    processDirect(callback::run)
    processDirectNN(callback::run)
    return "OK"
}
