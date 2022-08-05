fun box(): String {
    return try {
        bar()
        "FAIL"
    } catch (e: Throwable) {
        e.checkLinkageError("constructor Foo.<init> can not be called")
    }
}

private fun Throwable.checkLinkageError(prefix: String): String =
    if (this::class.simpleName == "IrLinkageError" && message?.startsWith("$prefix because it uses unlinked symbols") == true)
        "OK"
    else
        message!!
