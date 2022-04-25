fun test1(): String {
    try {
        return qux(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/exp_foo")) return "O"
    }

    return "FAIL2"
}

fun test2(): String = qux(false)

fun box(): String {
    return test1() + test2()
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
