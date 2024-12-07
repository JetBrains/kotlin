package helpers

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


val StateMachineChecker = StateMachineCheckerClass()

object CheckStateMachineContinuation: Continuation<Unit> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(value: Result<Unit>) {
        value.getOrThrow()
        StateMachineChecker.proceed = {
            StateMachineChecker.finished = true
        }
    }
}