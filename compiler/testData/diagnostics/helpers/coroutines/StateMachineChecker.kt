@file:JvmMultifileClass
@file:JvmName("CoroutineUtilKt")

package helpers

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

class StateMachineCheckerClass {
    private var counter = 0
    var finished = false

    var proceed: () -> Unit = {}

    fun reset() {
        counter = 0
        finished = false
        proceed = {}
    }

    suspend fun suspendHere() = suspendCoroutine<Unit> { c ->
        counter++
        proceed = { c.resume(Unit) }
    }

    fun check(numberOfSuspensions: Int, checkFinished: Boolean = true) {
        for (i in 1..numberOfSuspensions) {
            if (counter != i) error("Wrong state-machine generated: suspendHere should be called exactly once in one state. Expected " + i + ", got " + counter)
            proceed()
        }
        if (counter != numberOfSuspensions)
            error("Wrong state-machine generated: wrong number of overall suspensions. Expected " + numberOfSuspensions + ", got " + counter)
        if (finished) error("Wrong state-machine generated: it is finished early")
        proceed()
        if (checkFinished && !finished) error("Wrong state-machine generated: it is not finished yet")
    }
}

val StateMachineChecker = StateMachineCheckerClass()

object CheckStateMachineContinuation: ContinuationAdapter<Unit>() {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resume(value: Unit) {
        StateMachineChecker.proceed = {
            StateMachineChecker.finished = true
        }
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }
}
