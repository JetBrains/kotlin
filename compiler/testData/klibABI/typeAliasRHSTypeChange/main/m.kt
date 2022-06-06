fun box(): String {
    try {
        callFoo()
        return "FAIL1"
    } catch(e: Throwable) {
        if (!e.isLinkageError("/foo")) return "FAIL2"
    }

    return bar()
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
