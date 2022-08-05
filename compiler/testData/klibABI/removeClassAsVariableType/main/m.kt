fun box(): String {
    return try {
        bar()
        return "FAIL"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function bar can not be read")
    }
}

private fun Throwable.checkLinkageError(prefix: String): String =
    if (this::class.simpleName == "IrLinkageError" && message?.startsWith("$prefix because it uses unlinked symbols") == true)
        "OK"
    else
        message!!
