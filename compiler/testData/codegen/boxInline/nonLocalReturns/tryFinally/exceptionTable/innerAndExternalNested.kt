// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public class Exception1(message: String) : RuntimeException(message)

public class Exception2(message: String) : RuntimeException(message)

public inline fun doCall(block: ()-> String, exception1: (e: Exception)-> Unit, exception2: (e: Exception)-> Unit, finallyBlock: ()-> String,
                         exception3: (e: Exception)-> Unit, exception4: (e: Exception)-> Unit, finallyBlock2: ()-> String, res: String = "Fail") : String {
    try {
        try {
            block()
        }
        catch (e: Exception1) {
            exception1(e)
        }
        catch (e: Exception2) {
            exception2(e)
        }
        finally {
            finallyBlock()
        }
    } catch (e: Exception1) {
        exception3(e)
    }
    catch (e: Exception2) {
        exception4(e)
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

fun test0(h: Holder, throwEx1: Boolean, throwEx2: Boolean, throwEx3: Boolean = false, throwEx4: Boolean = false): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                if (throwEx1) {
                    throw Exception1("1")
                }
                if (throwEx2) {
                    throw Exception2("1")
                }
                return "OK_NON_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION1"
                if (throwEx3) {
                    throw Exception1("3_1")
                }
                if (throwEx4) {
                    throw Exception2("4_1")
                }
                return "OK_EXCEPTION1"
            },
            {
                h.value += ", OK_EXCEPTION2"
                if (throwEx3) {
                    throw Exception1("3_2")
                }
                if (throwEx4) {
                    throw Exception2("4_2")
                }
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY1"
                try {
                    try {
                        throw Exception1("fail")
                    }
                    catch (e: RuntimeException) {
                        h.value += ", CATCHED1"
                    }
                    finally {
                        h.value += ", ADDITIONAL"
                    }
                } finally {
                    h.value += " FINALLY1"
                }
                "OK_FINALLY1"
            },
            {
                h.value += ", OK_EXCEPTION3"
                return "OK_EXCEPTION3"
            },
            {
                h.value += ", OK_EXCEPTION4"
                return "OK_EXCEPTION4"
            },
            {
                h.value += ", OK_FINALLY2"
                try {
                    try {
                        throw Exception1("fail2")
                    } catch (e: RuntimeException) {
                        h.value += ", CATCHED2"
                    } finally {
                        h.value += ", ADDITIONAL"
                    }
                } finally {
                    h.value += " FINALLY2"
                }
                "OK_FINALLY2"
            })

    return localResult;

    return "FAIL";
}

fun box(): String {
    var h = Holder()
    var test0 = test0(h, false, false)
    if (test0 != "OK_NON_LOCAL" || h.value != "OK_NON_LOCAL, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_1: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false)
    if (test0 != "OK_EXCEPTION1" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_2: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true)
    if (test0 != "OK_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_3: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false, true, false)
    if (test0 != "OK_EXCEPTION3" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_EXCEPTION3, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_4: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, true, false, false, true)
    if (test0 != "OK_EXCEPTION4" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_EXCEPTION4, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_5: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true, true, false)
    if (test0 != "OK_EXCEPTION3" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_EXCEPTION3, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_6: ${test0}, holder: ${h.value}"

    h = Holder()
    test0 = test0(h, false, true, false, true)
    if (test0 != "OK_EXCEPTION4" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY1, CATCHED1, ADDITIONAL FINALLY1, OK_EXCEPTION4, OK_FINALLY2, CATCHED2, ADDITIONAL FINALLY2") return "test0_7: ${test0}, holder: ${h.value}"

    return "OK"
}
