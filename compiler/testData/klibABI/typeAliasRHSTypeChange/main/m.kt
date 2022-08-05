fun test1() = try {
    callFoo()
    "FAIL"
} catch (e: Throwable) {
    e.checkLinkageError("function foo can not be called")
}

fun test2() = bar()

fun box(): String = checkResults(test1(), test2())

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
