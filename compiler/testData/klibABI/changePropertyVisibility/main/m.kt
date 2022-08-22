fun test1(): String {
    return try {
        publicToInternalProperty1
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL1" }
    }
}

fun test2(): String {
    return try {
        publicToInternalProperty2
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL2" }
    }
}

fun test3(): String {
    return try {
        publicToPrivateProperty1
        "FAIL3"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")
    }
}

fun test4(): String {
    return try {
        publicToPrivateProperty2
        "FAIL4"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")
    }
}

fun test5(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedProperty1
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL5" }
    }
}

fun test6(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedProperty2
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL6" }
    }
}

fun test7(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalProperty1
        "FAIL7"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")
    }
}

fun test8(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalProperty2
        "FAIL8"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")
    }
}

fun test9(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateProperty1
        "FAIL9"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")
    }
}

fun test10(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateProperty2
        "FAIL10"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")
    }
}

fun test11(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedProperty1Access()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL11" }
    }
}

fun test12(): String {
    val c = ContainerImpl()
    return try {
        c.publicToProtectedProperty2Access()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL12" }
    }
}

fun test13(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalProperty1Access()
        "FAIL13"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToInternalProperty1.<get-publicToInternalProperty1> can not be called")
    }
}

fun test14(): String {
    val c = ContainerImpl()
    return try {
        c.publicToInternalProperty2Access()
        "FAIL14"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToInternalProperty2.<get-publicToInternalProperty2> can not be called")
    }
}

fun test15(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateProperty1Access()
        "FAIL15"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty1.<get-publicToPrivateProperty1> can not be called")
    }
}

fun test16(): String {
    val c = ContainerImpl()
    return try {
        c.publicToPrivateProperty2Access()
        "FAIL16"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor publicToPrivateProperty2.<get-publicToPrivateProperty2> can not be called")
    }
}

fun test17(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPublicProperty1Access()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL17" }
    }
}

fun test18(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPublicProperty2Access()
        "OK"
    } catch(e: Throwable) {
        e.message.orEmpty().ifBlank { "FAIL18" }
    }
}

fun test19(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToInternalProperty1Access()
        "FAIL19"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor protectedToInternalProperty1.<get-protectedToInternalProperty1> can not be called")
    }
}

fun test20(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToInternalProperty2Access()
        "FAIL20"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor protectedToInternalProperty2.<get-protectedToInternalProperty2> can not be called")
    }
}

fun test21(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPrivateProperty1Access()
        "FAIL21"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor protectedToPrivateProperty1.<get-protectedToPrivateProperty1> can not be called")
    }
}

fun test22(): String {
    val c = ContainerImpl()
    return try {
        c.protectedToPrivateProperty2Access()
        "FAIL22"
    } catch(e: Throwable) {
        e.checkLinkageError("property accessor protectedToPrivateProperty2.<get-protectedToPrivateProperty2> can not be called")
    }
}

fun box() = checkResults(
    test1(), test2(), test3(), test4(), test5(), test6(), test7(), test8(), test9(), test10(),
    test11(), test12(), test13(), test14(), test15(), test16(), test17(), test18(), test19(), test20(),
    test21(), test22())

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
