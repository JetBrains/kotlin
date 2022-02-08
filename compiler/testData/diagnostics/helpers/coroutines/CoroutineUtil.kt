package helpers

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
