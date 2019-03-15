// IGNORE_BACKEND: JVM_IR
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
// TREAT_AS_ONE_FILE

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

private var booleanResult = false
fun setBooleanRes(x: Boolean) {
    booleanResult = x
}

private var charResult: Char = '0'
fun setCharRes(x: Char) {
    charResult = x
}

private var byteResult: Byte = 0
fun setByteRes(x: Byte) {
    byteResult = x
}

private var shortResult: Short = 0
fun setShortRes(x: Short) {
    shortResult = x
}

private var intResult: Int = 0
fun setIntRes(x: Int) {
    intResult = x
}

fun box(): String {
    builder {
        val x = true
        suspendHere()
        setBooleanRes(x)
    }

    if (!booleanResult) return "fail 1"

    builder {
        val x = '1'
        suspendHere()
        setCharRes(x)
    }

    if (charResult != '1') return "fail 2"

    builder {
        val x: Byte = 1
        suspendHere()
        setByteRes(x)
    }

    if (byteResult != 1.toByte()) return "fail 3"

    builder {
        val x: Short = 1
        suspendHere()
        setShortRes(x)
    }

    if (shortResult != 1.toShort()) return "fail 4"

    builder {
        val x: Int = 1
        suspendHere()
        setIntRes(x)
    }

    if (intResult != 1) return "fail 5"
    return "OK"
}

// 1 PUTFIELD .*\.B\$0 : B
// 1 PUTFIELD .*\.C\$0 : C
// 1 PUTFIELD .*\.S\$0 : S
// 1 PUTFIELD .*\.Z\$0 : Z
// 1 PUTFIELD .*\.I\$0 : I
