// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public class Exception1(message: String) : RuntimeException(message)

public class Exception2(message: String) : RuntimeException(message)

public inline fun doCall(block: ()-> String, exception: (e: Exception)-> Unit, exception2: (e: Exception)-> Unit, finallyBlock: ()-> String, res: String = "Fail") : String {
    try {
        block()
    } catch (e: Exception1) {
        exception(e)
    } catch (e: Exception2) {
        exception2(e)
    } finally {
        finallyBlock()
    }
    return res
}

public inline fun <R> doCall2(block: ()-> R, exception: (e: Exception)-> Unit, finallyBlock: ()-> R) : R {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    } finally {
        finallyBlock()
    }
    throw RuntimeException("fail")
}

// FILE: 2.kt

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
                    h.value += ", OK_FINALLY"
                    throw RuntimeException("FINALLY")
                    "OK_FINALLY"
                })

        return localResult;
    }
    catch (e: RuntimeException) {
        if (e.message != "FINALLY") {
            return "FAIL in exception: " + e.message
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
                h.value += ", OK_FINALLY"
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
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })

    return localResult;
}

fun test1(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception1("FAIL")
                    "OK_LOCAL"
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
                    h.value += ", OK_FINALLY"
                    throw RuntimeException("FINALLY")
                    "OK_FINALLY"
                }, "Fail")
    }
    catch (e: RuntimeException) {
        if (e.message != "FINALLY") {
            return "FAIL in exception: " + e.message
        }
        else {
            return "CATCHED_EXCEPTION"
        }
    }

    return "FAIL";
}

fun test2(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception1("1")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    throw Exception2("2")
                    "OK_EXCEPTION"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    "OK_FINALLY"
                })
        return localResult;
    }
    catch (e: Exception2) {
        return "CATCHED_EXCEPTION"
    }

    return "Fail";
}

fun test3(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception2("FAIL")
                    "OK_LOCAL"
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
                    h.value += ", OK_FINALLY"
                    throw RuntimeException("FINALLY")
                    "OK_FINALLY"
                }, "Fail")
    }
    catch (e: RuntimeException) {
        if (e.message != "FINALLY") {
            return "FAIL in exception: " + e.message
        }
        else {
            return "CATCHED_EXCEPTION"
        }
    }

    return "FAIL";
}

fun test4(h: Holder): String {
    try {
        val localResult = doCall (
                {
                    h.value += "OK_LOCAL"
                    throw Exception2("1")
                    "OK_LOCAL"
                },
                {
                    h.value += ", OK_EXCEPTION1"
                    return "OK_EXCEPTION"
                },
                {
                    h.value += ", OK_EXCEPTION2"
                    throw Exception1("1")
                    "OK_EXCEPTION2"
                },
                {
                    h.value += ", OK_FINALLY"
                    "OK_FINALLY"
                })
        return localResult;
    }
    catch (e: Exception1) {
        return "CATCHED_EXCEPTION"
    }

    return "Fail";
}


fun test5(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_LOCAL"
                throw Exception2("FAIL")
                "OK_LOCAL"
            },
            {
                h.value += ", OK_EXCEPTION"
                throw RuntimeException("FAIL_EX")
                "OK_EXCEPTION"
            },
            {
                h.value += ", OK_EXCEPTION2"
                return "OK_EXCEPTION2"
            },
            {
                h.value += ", OK_FINALLY"
                "OK_FINALLY"
            })


    return localResult;
}

fun box(): String {
    var h = Holder()
    val test0 = test0(h)
    if (test0 != "CATCHED_EXCEPTION" || h.value != "OK_NON_LOCAL, OK_FINALLY") return "test0: ${test0}, holder: ${h.value}"

    h = Holder()
    val test01 = test01(h)
    if (test01 != "OK_EXCEPTION1" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY") return "test01: ${test01}, holder: ${h.value}"

    h = Holder()
    val test02 = test02(h)
    if (test02 != "OK_EXCEPTION2" || h.value != "OK_NON_LOCAL, OK_EXCEPTION2, OK_FINALLY") return "test02: ${test02}, holder: ${h.value}"


    h = Holder()
    val test1 = test1(h)
    if (test1 != "CATCHED_EXCEPTION" || h.value != "OK_LOCAL, OK_EXCEPTION1, OK_FINALLY") return "test1: ${test1}, holder: ${h.value}"

    h = Holder()
    val test2 = test2(h)
    if (test2 != "CATCHED_EXCEPTION" || h.value != "OK_LOCAL, OK_EXCEPTION1, OK_FINALLY") return "test2: ${test2}, holder: ${h.value}"


    h = Holder()
    val test3 = test3(h)
    if (test3 != "CATCHED_EXCEPTION" || h.value != "OK_LOCAL, OK_EXCEPTION2, OK_FINALLY") return "test3: ${test3}, holder: ${h.value}"

    h = Holder()
    val test4 = test4(h)
    if (test4 != "CATCHED_EXCEPTION" || h.value != "OK_LOCAL, OK_EXCEPTION2, OK_FINALLY") return "test4: ${test4}, holder: ${h.value}"

    h = Holder()
    val test5 = test5(h)
    if (test5 != "OK_EXCEPTION2" || h.value != "OK_LOCAL, OK_EXCEPTION2, OK_FINALLY") return "test5: ${test5}, holder: ${h.value}"

    return "OK"
}
