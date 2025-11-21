// KT-79359
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_BACKEND: JS_IR_ES6
// ^ Regression with capturing of outer this in private members
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

private fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class First {
    val o = "O"

    inner class Second {
        val k = "K"

        private suspend fun suspendHereAndContinue(): String = suspendCoroutineUninterceptedOrReturn {
            it.resume(this@First.o + this.k)
            COROUTINE_SUSPENDED
        }

        suspend fun getResult() = suspendHereAndContinue()
    }
}


fun box(): String {
    var result = "Nothing was called"

    builder {
        val first = First()
        val second = first.Second()
        result = second.getResult()
    }

    return result
}