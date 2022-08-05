fun test1(): String {
    return try {
        fooVariableType()
        "FAIL1"
    } catch (e: Throwable) {
        e.checkLinkageError("val foo can not be read")
    }
}

fun test2(): String {
    return try {
        barVariableType()
        "FAIL2"
    } catch (e: Throwable) {
        e.checkLinkageError("val bar can not be read")
    }
}

fun test3(): String {
    return try {
        fooInstance()
        "FAIL3"
    } catch (e: Throwable) {
        e.checkLinkageError("constructor Foo.<init> can not be called")
    }
}

fun test4(): String {
    return try {
        barInstance()
        "FAIL4"
    } catch (e: Throwable) {
        e.checkLinkageError("constructor Bar.<init> can not be called")
    }
}

fun test5(): String {
    return try {
        fooInstance2()
        "FAIL5"
    } catch (e: Throwable) {
        e.checkLinkageError("reference to constructor Foo.<init> can not be evaluated")
    }
}

fun test6(): String {
    return try {
        barInstance2()
        "FAIL6"
    } catch (e: Throwable) {
        e.checkLinkageError("reference to constructor Bar.<init> can not be evaluated")
    }
}

fun test7(): String {
    return try {
        fooAnonymousObject()
        "FAIL7"
    } catch (e: Throwable) {
        e.checkLinkageError("val foo can not be read")
    }
}

fun box(): String = checkResults(test1(), test2(), test3(), test4(), test5(), test6(), test7())

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
