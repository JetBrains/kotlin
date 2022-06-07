fun box(): String {
    try {
        return bar()
    } catch(e: Throwable) {
        if (e.isLinkageError("/foo.<get-foo>")) return "OK"
    }

    return "FAIL2"
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
