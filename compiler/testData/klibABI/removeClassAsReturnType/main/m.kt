fun test1(d: D): String {
    try {
        d.barF()
    } catch(e: Throwable) {
        if (e.isLinkageError("/D.expF")) return "O"
    }

    return "FAIL3"
}

fun test2(d: D): String {
    return d.fooF()
}

fun test3(d: D): String {
    try {
        d.barP1
    } catch(e: Throwable) {
        if (e.isLinkageError("/D.expP1.<get-expP1>")) return "O"
    }

    return "FAIL4"
}

fun test4(d: D): String {
    return d.fooP1
}

fun test5(): String {
    try {
        D2().barP2
    } catch(e: Throwable) {
        if (e.isLinkageError("/D2.expP2.<get-expP2>")) return "OK"
        else throw e
    }

    return "FAIL5"
}

fun box(): String {
    val result = test1(D()) + test2(D()) + test3(D()) + test4(D()) + test5()
    return if (result == "OKOKOK") "OK" else result
}

private fun Throwable.isLinkageError(symbolName: String): Boolean =
    this::class.simpleName == "IrLinkageError" && message?.startsWith("Unlinked type in signature of IR symbol $symbolName|") == true
