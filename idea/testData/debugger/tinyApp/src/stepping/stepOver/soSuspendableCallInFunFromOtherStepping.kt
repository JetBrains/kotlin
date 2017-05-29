package soSuspendableCallInFunFromOtherStepping

import forTests.builder
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

private fun foo(a: Any) {}

fun main(args: Array<String>) {
    builder {
        inFun()
    }

    foo("Main end")
    Thread.sleep(120)
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
            cont.resume(Unit)
            Thread.sleep(10)
        }.start()
    }
}

// STEP_OVER: 3