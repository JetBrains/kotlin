fun test1(d: D): String {
    return try {
        d.bar()
    } catch(e: Throwable) {
        e.checkLinkageError("constructor E.<init> can not be called")
    }
}

fun test2(d: D): String {
    return d.foo()
}

fun box(): String =checkResults(test1(D()), test2(D()))

private fun Throwable.checkLinkageError(prefix: String): String {
    if (this::class.simpleName != "IrLinkageError") return "Unexpected throwable: ${this::class}"

    val expectedMessagePrefix = "$prefix because it uses unlinked symbols"
    val actualMessage = message.orEmpty()

    return if (actualMessage.startsWith(expectedMessagePrefix))
        "OK"
    else
        "EXPECTED: $expectedMessagePrefix, ACTUAL: $actualMessage"
}

private fun checkResults(vararg results: String): String = when {
    results.isEmpty() -> "no results to check"
    results.all { it == "OK" } -> "OK"
    else -> results.joinToString("\n")
}
