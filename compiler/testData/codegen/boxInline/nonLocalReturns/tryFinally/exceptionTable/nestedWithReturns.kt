// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public class Exception1(message: String) : RuntimeException(message)

public class Exception2(message: String) : RuntimeException(message)

public inline fun doCall(block: ()-> String, exception1: (e: Exception)-> Unit, finallyBlock: ()-> String,
                         exception3: (e: Exception)-> Unit, finallyBlock2: ()-> String, res: String = "Fail") : String {
    try {
        try {
            block()
        }
        catch (e: Exception1) {
            exception1(e)
        }
        finally {
            if (true) {
                finallyBlock()
                /*External finally would be injected here*/
                return res + "_INNER_FINALLY"
            }
        }
    } catch (e: Exception2) {
        exception3(e)
    }
    finally {
        finallyBlock2()
    }
    return res
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}

fun test0(
        h: Holder,
        throwExceptionInTry: Boolean,
        throwInternalEx2: Boolean = false,
        throwInternalFinEx1: Boolean = false,
        throwInternalFinEx2: Boolean = false,
        throwExternalFinEx1: Boolean = false,
        throwExternalFinEx2: Boolean = false,
        res: String = "Fail"
): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_NON_LOCAL"
                    if (throwExceptionInTry) {
                        throw Exception1("1")
                    }
                    return "OK_NON_LOCAL"
                },
                {
                    h.value += ", OK_INTERNAL_EXCEPTION1"
                    if (throwInternalEx2) {
                        throw Exception2("2_1")
                    }
                    return "OK_INTERNAL_EXCEPTION1"
                },
                {
                    h.value += ", OK_FINALLY1"
                    if (throwInternalFinEx1) {
                        throw Exception1("EXCEPTION_IN_INTERNAL_FINALLY")
                    }
                    if (throwInternalFinEx2) {
                        throw Exception2("EXCEPTION222_IN_INTERNAL_FINALLY")
                    }
                    "OK_FINALLY1"
                },
                {
                    h.value += ", OK_EXTERNAL_EXCEPTION2"
                    return "OK_EXTERNAL_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY2"
                    if (throwExternalFinEx1) {
                        throw Exception1("EXCEPTION_IN_EXTERNAL_FINALLY")
                    }
                    if (throwExternalFinEx2) {
                        throw Exception2("EXCEPTION222_IN_EXTERNAL_FINALLY")
                    }
                    "OK_FINALLY2"
                }, res)
        return localResult;
    } catch(e: Exception1) {
        return e.message!!
    } catch(e: Exception2) {
        return e.message!!
    }
}

fun box(): String {
    var h = Holder()
    var test0 = test0(h, false, throwExternalFinEx1 = false, res = "OK")
    if (test0 != "OK_INNER_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_1: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, throwExternalFinEx1 = true, res = "OK")
    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_2: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, throwExternalFinEx2 = true, res = "OK")
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_4: ${test0}, holder: ${h.value}"




    h = Holder()
    test0 = test0(h, true, throwExternalFinEx1 = true, res = "OK")
    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_3: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, throwInternalEx2 = true, throwExternalFinEx1 = true, res = "OK")
    if (test0 != "EXCEPTION_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_5: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, throwInternalEx2 = true, throwExternalFinEx2 = true, res = "OK")
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_6: ${test0}, holder: ${h.value}"



    h = Holder()
    test0 = test0(h, false, throwInternalFinEx1 = true)
    if (test0 != "EXCEPTION_IN_INTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_7: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, throwInternalFinEx1 = true, throwExternalFinEx2 = true)
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_FINALLY2") return "test0_71: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, throwInternalFinEx2 = true)
    if (test0 != "OK_EXTERNAL_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_8: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, throwInternalFinEx2 = true, throwExternalFinEx2 = true)
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_81: ${test0}, holder: ${h.value}"



    h = Holder()
    test0 = test0(h, true, throwInternalFinEx1 = true)
    if (test0 != "EXCEPTION_IN_INTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_9: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, throwInternalFinEx1 = true, throwExternalFinEx2 = true)
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_FINALLY2") return "test0_10: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, throwInternalFinEx2 = true)
    if (test0 != "OK_EXTERNAL_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_11: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, throwInternalFinEx2 = true, throwExternalFinEx2 = true)
    if (test0 != "EXCEPTION222_IN_EXTERNAL_FINALLY" || h.value != "OK_NON_LOCAL, OK_INTERNAL_EXCEPTION1, OK_FINALLY1, OK_EXTERNAL_EXCEPTION2, OK_FINALLY2") return "test0_12: ${test0}, holder: ${h.value}"

    return "OK"
}
