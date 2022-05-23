fun test1(): String {
    try {
        return qux(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/exp_foo")) return "OK"
    }

    return "FAIL5"
}

fun test2(): String = qux(false)

fun test3(): String {
    try {
        return qux2(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/A.exp_foo")) return "OK"
    }

    return "FAIL6"
}

fun test4(): String = qux2(false)

fun box(): String {
    val result = test1() + test2() + test3() + test4()
    return if (result == "OKOKOKOK") "OK" else result
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
