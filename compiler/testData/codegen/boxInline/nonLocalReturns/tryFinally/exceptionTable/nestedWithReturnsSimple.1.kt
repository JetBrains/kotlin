import test.*

class Holder {
    var value: String = ""
}

fun test0(
        h: Holder,
        throwExternalFinEx1: Boolean = false,
        res: String = "Fail"
): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_NON_LOCAL"
                    return "OK_NON_LOCAL"
                },
                {
                    h.value += ", OK_FINALLY1"
                    "OK_FINALLY1"
                },
                {
                    h.value += ", OK_FINALLY2"
                    if (throwExternalFinEx1) {
                        throw Exception1("EXCEPTION_IN_EXTERNAL_FINALLY")
                    }
                    "OK_FINALLY2"
                }, res)
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
    if (test0 != "OK_INNER_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_1: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, throwExternalFinEx1 = true, res = "OK")
    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_2: ${test0}, holder: ${h.value}"

//    h = Holder()
//    test0 = test0(h, throwExternalFinEx2 = true, res = "OK")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_4: ${test0}, holder: ${h.value}"




//    h = Holder()
//    test0 = test0(h, true, throwExternalFinEx1 = true, res = "OK")
//    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_3: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalEx2 = true, throwExternalFinEx1 = true, res = "OK")
//    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_5: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalEx2 = true, throwExternalFinEx2 = true, res = "OK")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_6: ${test0}, holder: ${h.value}"
//
//
//
//    h = Holder()
//    test0 = test0(h, false, throwInternalFinEx1 = true, res = "FAIL")
//    if (test0 != "EXCEPTION_IN_INTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_7: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, false, throwInternalFinEx1 = true, throwExternalFinEx2 = true, res = "FAIL")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_71: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, false, throwInternalFinEx2 = true, res = "FAIL")
//    if (test0 != "OK_EXTERNAL_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_8: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, false, throwInternalFinEx2 = true, throwExternalFinEx2 = true, res = "FAIL")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_81: ${test0}, holder: ${h.value}"
//
//
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalFinEx1 = true, res = "FAIL")
//    if (test0 != "EXCEPTION_IN_INTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_9: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalFinEx1 = true, throwExternalFinEx2 = true, res = "FAIL")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_10: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalFinEx2 = true, res = "FAIL")
//    if (test0 != "OK_EXTERNAL_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_11: ${test0}, holder: ${h.value}"
//
//    h = Holder()
//    test0 = test0(h, true, throwInternalFinEx2 = true, throwExternalFinEx2 = true, res = "FAIL")
//    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_12: ${test0}, holder: ${h.value}"

    return "OK"
}
