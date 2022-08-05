fun test1(): String {
    return try {
        qux(true)
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor exp_foo.<get-exp_foo> can not be called")
    }
}

fun test2(): String = qux(false)

fun test3(): String {
    return try {
        qux2(true)
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor A.exp_foo.<get-exp_foo> can not be called")
    }
}

fun test4(): String = qux2(false)

fun test5(): String {
    return try {
        return qux3(true)
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor B.exp_foo.<get-exp_foo> can not be called")
    }
}

fun test6(): String = qux3(false)

fun box(): String = checkResults(test1(), test2(), test3(), test4(), test5(), test6())

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
