fun test1(d: D): String {
    return try {
        d.barF()
    } catch(e: Throwable) {
        e.checkLinkageError("function expF can not be called")
    }
}

fun test2(d: D): String {
    return d.fooF()
}

fun test3(d: D): String {
    return try {
        d.barP1
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor expP1.<get-expP1> can not be called")
    }
}

fun test4(d: D): String {
    return d.fooP1
}

fun test5(): String {
    return try {
        D2().barP2
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor expP2.<get-expP2> can not be called")
    }
}

fun test6(): String {
    return try {
        bar()
        return "FAIL6"
    } catch (e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

fun test7(): String {
    return try {
        baz()
        return "FAIL7"
    } catch (e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

fun test8(): String {
    return try {
        quux()
        return "FAIL8"
    } catch (e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

fun test9(): String {
    return try {
        grault()
        return "FAIL9"
    } catch (e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

fun test10(): String {
    return try {
        waldo()
        return "FAIL10"
    } catch (e: Throwable) {
        e.checkLinkageError("function foo can not be called")
    }
}

fun box(): String = checkResults(test1(D()), test2(D()), test3(D()), test4(D()), test5(), test6(), test7(), test8(), test9(), test10())

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
