fun test1(d: D): String {
    return try {
        d.bar()
    } catch(e: Throwable) {
        e.checkLinkageError("function D.exp can not be called")
    }
}

fun test2(d: D): String {
    return d.foo()
}

fun box(): String =checkResults(test1(D()), test2(D()))

private fun Throwable.checkLinkageError(prefix: String): String =
    if (this::class.simpleName == "IrLinkageError" && message?.startsWith("$prefix because it uses unlinked symbols") == true)
        "OK"
    else
        message!!

private fun checkResults(vararg results: String): String = when {
    results.isEmpty() -> "no results to check"
    results.all { it == "OK" } -> "OK"
    else -> results.joinToString("\n")
}
