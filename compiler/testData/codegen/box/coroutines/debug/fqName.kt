// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// WITH_COROUTINES

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package some.llong.name

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

suspend fun dummy() {}

class Test {
    suspend fun getStackTraceElement(): StackTraceElement {
        dummy() // to force state-machine generation
        return suspendCoroutineUninterceptedOrReturn<StackTraceElement> {
            (it as BaseContinuationImpl).getStackTraceElement()
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "OK"
    builder {
        if (Test().getStackTraceElement().className != "some.llong.name.Test") {
            res = Test().getStackTraceElement().className
        }
    }
    return res
}
