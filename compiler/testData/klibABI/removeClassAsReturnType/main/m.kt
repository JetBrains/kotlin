fun test1(d: D): String {
    return try {
        d.barF()
    } catch(e: Throwable) {
        e.checkLinkageError("function D.expF can not be called")
    }
}

fun test2(d: D): String {
    return d.fooF()
}

fun test3(d: D): String {
    return try {
        d.barP1
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor D.expP1.<get-expP1> can not be called")
    }
}

fun test4(d: D): String {
    return d.fooP1
}

fun test5(): String {
    return try {
        D2().barP2
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor D2.expP2.<get-expP2> can not be called")
    }
}

fun box(): String = checkResults(test1(D()), test2(D()), test3(D()), test4(D()), test5())

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
