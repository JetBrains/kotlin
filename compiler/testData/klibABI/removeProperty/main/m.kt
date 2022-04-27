fun test1(): String {
    try {
        return qux(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/exp_foo.<get-exp_foo>")) return "OK"
    }

    return "FAIL7"
}

fun test2(): String = qux(false)

fun test3(): String {
    try {
        return qux2(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/A.exp_foo.<get-exp_foo>")) return "OK"
    }

    return "FAIL8"
}

fun test4(): String = qux2(false)

fun test5(): String {
    try {
        return qux3(true)
    } catch(e: Throwable) {
        if (e.isLinkageError("/B.exp_foo.<get-exp_foo>")) return "OK"
    }

    return "FAIL9"
}

fun test6(): String = qux3(false)

fun box(): String {
    val result = test1() + test2() + test3() + test4() + test5() + test6()
    return if (result == "OKOKOKOKOKOK") "OK" else result
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked IR symbol $symbolName|") == true
