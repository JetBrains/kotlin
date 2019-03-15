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

fun box(): String {
    builder {
        val x = true
        suspendHere()
        val y: Boolean = x
        if (!y) throw IllegalStateException("fail 1")
    }

    builder {
        val x = '1'
        suspendHere()

        val y: Char = x
        if (y != '1') throw IllegalStateException("fail 2")
    }

    builder {
        val x: Byte = 1
        suspendHere()

        val y: Byte = x
        if (y != 1.toByte()) throw IllegalStateException("fail 3")
    }

    builder {
        val x: Short = 1

        suspendHere()

        val y: Short = x
        if (y != 1.toShort()) throw IllegalStateException("fail 4")
    }

    builder {
        val x: Int = 1
        suspendHere()

        val y: Int = x
        if (y != 1) throw IllegalStateException("fail 5")
    }

    return "OK"
}

// 1 PUTFIELD .*\.B\$0 : B
// 1 PUTFIELD .*\.C\$0 : C
// 1 PUTFIELD .*\.S\$0 : S
// 1 PUTFIELD .*\.Z\$0 : Z
// 1 PUTFIELD .*\.I\$0 : I
