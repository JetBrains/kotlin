// JVM_ABI_K1_K2_DIFF: KT-63861

// FILE: 1.kt

package test

public class Exception1(message: String) : RuntimeException(message)

public inline fun doCall(block: ()-> String, exception: (e: Exception)-> Unit, finallyBlock: ()-> String, res: String = "Fail") : String {
    try {
        block()
    } catch (e: Exception1) {
        exception(e)
    } finally {
        finallyBlock()
    }
    return res
}

// FILE: 2.kt

import test.*

class Holder {
    var value: String = ""
}

fun test01(h: Holder): String {
    val localResult = doCall (
            {
                h.value += "OK_NON_LOCAL"
                throw Exception1("1")
                "OK_NON_LOCAL_RES"
            },
            {
                h.value += ", OK_EXCEPTION1"
                return "OK_EXCEPTION1"
            },
            {
                try {
                    h.value += ", OK_FINALLY"
                    throw RuntimeException("FINALLY")
                } catch(e: RuntimeException) {
                    h.value += ", OK_CATCHED"
                }
                "OK_FINALLY_RES"
            }, "FAIL")

    return localResult;
}
fun box(): String {
    var h = Holder()
    val test01 = test01(h)
    if (test01 != "OK_EXCEPTION1" || h.value != "OK_NON_LOCAL, OK_EXCEPTION1, OK_FINALLY, OK_CATCHED") return "test01: ${test01}, holder: ${h.value}"

    return "OK"
}
