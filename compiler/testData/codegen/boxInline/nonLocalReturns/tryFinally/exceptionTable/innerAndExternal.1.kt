import test.*

class Holder {
    var value: String = ""
}

fun test0(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_NON_LOCAL"
                    return "OK_NON_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    return "OK_EXCEPTION1"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    return "OK_EXCEPTION2"
                },
                {
                    try {
                        h.value += ", OK_FINALLY"
                        throw java.lang.RuntimeException("FINALLY")
                        "OK_FINALLY"
                    } finally {
                        h.value += ", OK_FINALLY_INNER"
                    }
                })

        return localResult;
    }
    catch (e: RuntimeException) {
        if (e.getMessage() != "FINALLY") {
            return "FAIL in exception: " + e.getMessage()
        }
        else {
            return "CATCHED_EXCEPTION"
        }
    }

    return "FAIL";
}

fun test01(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                throw Exception1("1")
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                try {
                    h.value += ", OK_FINALLY"
                    throw java.lang.RuntimeException("FINALLY")
                } catch(e: RuntimeException) {
                    h.value += ", OK_CATCHED"
                } finally {
                    h.value += ", OK_FINALLY_INNER"
                }
                "OK_FINALLY"
            })

    return localResult;
}

fun test02(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                throw Exception2("1")
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                try {
                    h.value += ", OK_FINALLY"
                    throw java.lang.RuntimeException("FINALLY")
                } catch(e: RuntimeException) {
                    h.value += ", OK_CATCHED"
                } finally {
                    h.value += ", OK_FINALLY_INNER"
                }
                "OK_FINALLY"
            }, "OK")

    return localResult;
}

fun box(): String {
    var h = Holder()
    val test0 = test0(h)
    if (test0 != "CATCHED_EXCEPTION" || h.value != "OK_NON_LOCAL, OK_FINALLY, OK_FINALLY_INNER") return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test01 = test01(h)
    if (test01 != "OK_EXCEPTION1" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY, OK_CATCHED, OK_FINALLY_INNER") return "test01: ${test01}, holder: ${h.value}"

    h = Holder()
    val test02 = test02(h)
    if (test02 != "OK_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY, OK_CATCHED, OK_FINALLY_INNER") return "test02: ${test02}, holder: ${h.value}"

    return "OK"
}
