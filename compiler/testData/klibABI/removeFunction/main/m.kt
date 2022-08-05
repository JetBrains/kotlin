fun test1(): String {
    return try {
        qux(true)
    } catch(e: Throwable) {
        e.checkLinkageError("function exp_foo can not be called")
    }
}

fun test2(): String = qux(false)

fun test3(): String {
    return try {
        qux2(true)
    } catch(e: Throwable) {
        e.checkLinkageError("function A.exp_foo can not be called")
    }
}

fun test4(): String = qux2(false)

fun box() = checkResults(test1(), test2(), test3(), test4())

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
