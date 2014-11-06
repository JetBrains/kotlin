import test.*


fun test0(
        h: Holder,
        throwExternalFinEx1: Boolean = false,
        res: String = "Fail"
): String {
    try {
        val localResult = doCall (
                {
                    h += "OK_NON_LOCAL"
                    return "OK_NON_LOCAL"
                },
                {
                    h += "OK_FINALLY1"
                    "OK_FINALLY1"
                },
                {
                    h += "innerTryBlock"
                    if (throwExternalFinEx1) {
                        throw Exception1("EXCEPTION_IN_EXTERNAL_FINALLY")
                    }
                    "innerTryBlock"
                },
                {
                    h += "CATCHBLOCK"
                    "CATCHBLOCK"
                },
                res)
        return localResult;
    } catch(e: Exception1) {
        return e.getMessage()!!
    } catch(e: Exception2) {
        return e.getMessage()!!
    }
}

fun box(): String {
    var h = Holder()
    var test0 = test0(h, res = "OK")
    if (test0 != "OK_NON_LOCAL" || h.value != "OK_NON_LOCAL -> OK_FINALLY1 -> innerTryBlock") return "test0_1: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, throwExternalFinEx1 = true, res = "OK")
    if (test0 != "OK_NON_LOCAL" || h.value != "OK_NON_LOCAL -> OK_FINALLY1 -> innerTryBlock -> CATCHBLOCK") return "test0_2: ${test0}, holder: ${h.value}"

    return "OK"
}
