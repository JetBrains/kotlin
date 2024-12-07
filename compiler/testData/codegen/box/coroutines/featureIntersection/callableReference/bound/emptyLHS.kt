// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

var result = ""

class A {
    suspend fun memberFunction() {
        result += "A.mf,"
    }

    suspend fun aMemberFunction() {
        result += "A.amf,"
    }

    suspend fun test(): String {
        (::memberFunction).let { it() }
        (::aExtensionFunction).let { it() }

        return result
    }

    inner class B {
        suspend fun memberFunction() {
            result += "B.mf,"
        }

        suspend fun test(): String {
            (::aMemberFunction).let { it() }
            (::aExtensionFunction).let { it() }

            (::memberFunction).let { it() }

            (::bExtensionFunction).let { it() }

            return result
        }
    }
}

suspend fun A.aExtensionFunction() {
    result += "A.ef,"
}

suspend fun A.B.bExtensionFunction() {
    result += "B.ef,"
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var a = "FAIL 1"
    builder {
        a = A().test()
    }
    if (a != "A.mf,A.ef,") return "Fail $a"

    result = ""
    var b = "FAIL 2"
    builder {
        b = A().B().test()
    }
    if (b != "A.amf,A.ef,B.mf,B.ef,") return "Fail $b"

    result = ""
    builder {
        with(A()) {
            (::memberFunction).let { it() }
            (::aExtensionFunction).let { it() }
        }
    }
    if (result != "A.mf,A.ef,") return "Fail $result"

    result = ""
    builder {
        with(A()) {
            with(B()) {
                (::aMemberFunction).let { it() }
                (::aExtensionFunction).let { it() }

                (::memberFunction).let { it() }

                (::bExtensionFunction).let { it() }
            }
        }
    }
    if (result != "A.amf,A.ef,B.mf,B.ef,") return "Fail $result"

    return "OK"
}
