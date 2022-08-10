fun test1(): String {
    return try {
        bar()
        return "FAIL1"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function bar can not be read")
    }
}

fun test2(): String {
    return try {
        baz()
        return "FAIL2"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function qux declared in function baz can not be read")
    }
}

fun test3(): String {
    return try {
        quux()
        return "FAIL3"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function quux\$Local.corge can not be read")
    }
}

fun test4(): String {
    return try {
        grault()
        return "FAIL4"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function grault\$1.garply can not be read")
    }
}

fun test5(): String {
    return try {
        waldo()
        return "FAIL5"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo declared in function waldo\$fred\$1.garply")
    }
}

fun box() = checkResults(test1(), test2(), test3(), test4(), test5())

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
