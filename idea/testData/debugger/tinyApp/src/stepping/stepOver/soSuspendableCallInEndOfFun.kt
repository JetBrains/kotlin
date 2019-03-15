package soSuspendableCallInEndOfFun

import forTests.WaitFinish
import forTests.builder
import kotlin.coroutines.Continuation
import kotlin.coroutines.*

private fun foo(a: Any) {}

val waiter = WaitFinish()
val suspendWaiter = WaitFinish()

fun main(args: Array<String>) {
    builder {
        inFun()
    }

    suspendWaiter.finish()

    foo("Main end")
    waiter.waitEnd()
}

suspend fun inFun() {
    foo("Start")
    //Breakpoint!
    run()
}

suspend fun run() {
    suspendCoroutine { cont: Continuation<Unit> ->
        Thread {
            suspendWaiter.waitEnd()
            cont.resume(Unit)
            waiter.finish()
        }.start()
    }
}

// STEP_OVER: 4