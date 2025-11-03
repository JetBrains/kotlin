// KT-79359
// TARGET_BACKEND: JS_IR, JS_IR_ES6
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

var effects = ""

private fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun suspendHere(): Unit = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

suspend fun simpleReturnsUnit() {
    effects += "[simpleReturnsUnit]"
}

suspend fun callTailReturnsUnit() {
    effects += "[callTailReturnsUnit]"
    return suspendHere()
}

suspend fun complexReturnsUnit(shouldSuspend: Boolean) {
   if (shouldSuspend)  {
       suspendHere()
   }

   effects += "[complexReturnsUnit]"
}

fun box(): String {
    var failReason: String? = null

    builder {
        val simpleUnit = simpleReturnsUnit()
        if (simpleUnit.toString() != "kotlin.Unit") {
            failReason = "simpleReturnsUnit returns not Unit, but $simpleUnit"
            return@builder
        }

        val tailcallUnit = callTailReturnsUnit()
        if (tailcallUnit.toString() != "kotlin.Unit") {
            failReason = "callTailReturnsUnit returns not Unit, but $tailcallUnit"
            return@builder
        }

        val complexUnit = complexReturnsUnit(shouldSuspend = true)
        if (complexUnit.toString() != "kotlin.Unit") {
            failReason = "complexReturnsUnit returns not Unit, but $complexUnit"
            return@builder
        }
    }

    return when {
        failReason != null -> failReason
        effects != "[simpleReturnsUnit][callTailReturnsUnit][complexReturnsUnit]" -> "Fail: effects are $effects"
        else -> "OK"
    }
}