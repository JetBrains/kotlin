fun test1(d: D): String {
    try {
        d.bar()
    } catch(e: Throwable) {
        if (e.isLinkageError("/D.exp")) return "O"
    }

    return "FAIL2"
}

fun test2(d: D): String {
    return d.foo()
}

fun box(): String {
    return test1(D()) + test2(D())
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked type in signature of IR symbol $symbolName|") == true
