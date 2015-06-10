import test.*


fun test0(h: Holder, throwException: Boolean): Int {
    val localResult = doCall2_2 (
            {
                h.value += "OK_NONLOCAL"
                if (throwException) {
                    throw java.lang.RuntimeException()
                }
                return 1
            },
            "FAIL",
            h
    )

    return -1;
}

fun box(): String {
    var h = Holder()
    val test0 = test0(h, true)
    if (test0 != -1 || h.value != "OK_NONLOCAL, OK_EXCEPTION, OK_FINALLY, DO_CALL_2_FINALLY, DO_CALL_2_1_FINALLY, DO_CALL_2_2_FINALLY")
        return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test1 = test0(h, false)
    if (test1 != 1 || h.value != "OK_NONLOCAL, OK_FINALLY, DO_CALL_2_FINALLY, DO_CALL_2_1_FINALLY, DO_CALL_2_2_FINALLY")
        return "test1: ${test1}, holder: ${h.value}"

    return "OK"
}


