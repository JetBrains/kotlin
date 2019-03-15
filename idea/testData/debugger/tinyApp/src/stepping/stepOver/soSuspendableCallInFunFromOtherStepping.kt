package soSuspendableCallInFunFromOtherStepping

import forTests.builder
import forTests.WaitFinish
import kotlin.coroutines.Continuation
import kotlin.coroutines.*

private fun foo(a: Any) {}

val waiter = WaitFinish()

fun main(args: Array<String>) {
    builder {
        inFun()
    }

    foo("Main end")
    waiter.waitEnd()
}

suspend fun inFun() {
    //Breakpoint!
    foo("Start")
    run()
    foo("End")
}

suspend fun run() {
    return suspendCoroutine { cont: Continuation<Unit> ->
        Thread {
            Thread.sleep(10)
            cont.resume(Unit)
            waiter.finish()
        }.start()
    }
}

// STEP_OVER: 3