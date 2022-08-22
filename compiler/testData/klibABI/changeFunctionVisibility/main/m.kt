fun test1(): String {
    return try {
        publicToInternalFunction()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL1" }
    }
}

fun test2(): String {
    return try {
        publicToPrivateFunction()
        "FAIL2"
    } catch(e: Throwable) {
        e.checkLinkageError("function publicToPrivateFunction can not be called")
    }
}

fun test3(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedFunction()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL3" }
    }
}

fun test4(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalFunction()
        "FAIL4"
    } catch(e: Throwable) {
        e.checkLinkageError("function publicToInternalFunction can not be called")
    }
}

fun test5(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateFunction()
        "FAIL5"
    } catch(e: Throwable) {
        e.checkLinkageError("function publicToPrivateFunction can not be called")
    }
}

fun test6(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedFunctionAccess()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL6" }
    }
}

fun test7(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalFunctionAccess()
        "FAIL7"
    } catch(e: Throwable) {
        e.checkLinkageError("function publicToInternalFunction can not be called")
    }
}

fun test8(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateFunctionAccess()
        "FAIL8"
    } catch(e: Throwable) {
        e.checkLinkageError("function publicToPrivateFunction can not be called")
    }
}

fun test9(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPublicFunctionAccess()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL9" }
    }
}

fun test10(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToInternalFunctionAccess()
        "FAIL10"
    } catch(e: Throwable) {
        e.checkLinkageError("function protectedToInternalFunction can not be called")
    }
}

fun test11(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPrivateFunctionAccess()
        "FAIL11"
    } catch(e: Throwable) {
        e.checkLinkageError("function protectedToPrivateFunction can not be called")
    }
}

fun box() = checkResults(test1(), test2(), test3(), test4(), test5(), test6(), test7(), test8(), test9(), test10(), test11())

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
