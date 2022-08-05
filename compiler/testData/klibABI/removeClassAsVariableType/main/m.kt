fun test1(): String {
    return try {
        bar()
        return "FAIL1"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo can not be read")
    }
}

fun test2(): String {
    return try {
        baz()
        return "FAIL2"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo can not be read")
    }
}

fun test3(): String {
    return try {
        quux()
        return "FAIL3"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo can not be read")
    }
}

fun test4(): String {
    return try {
        grault()
        return "FAIL4"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo can not be read")
    }
}

fun test5(): String {
    return try {
        waldo()
        return "FAIL5"
    } catch (e: Throwable) {
        e.checkLinkageError("var foo can not be read")
    }
}

fun box() = checkResults(test1(), test2(), test3(), test4(), test5())

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
