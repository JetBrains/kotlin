fun box(): String {
    return try {
        bar()
    } catch(e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

private fun Throwable.checkLinkageError(prefix: String): String =
    if (this::class.simpleName == "IrLinkageError" && message?.startsWith("$prefix because it uses unlinked symbols") == true)
        "OK"
    else
        message!!
