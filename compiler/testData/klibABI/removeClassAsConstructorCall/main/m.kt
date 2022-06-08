fun box(): String {
    try {
        bar()
        return "FAIL1"
    } catch (e: Throwable) {
        if (!e.isLinkageError("/Foo.<init>")) return "FAIL2"
    }

    return "OK"
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
