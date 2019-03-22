@file:Suppress("UNUSED_VARIABLE")

import kotlin.coroutines.*
import java.lang.Thread.sleep

class LambdaAssignmentCheck {
    fun returnSuspend(): suspend () -> Unit = {
        Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(1)
    }

    fun assignToSuspendType() {
        val suspendType: suspend () -> Unit = {
            Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(2)
        }
    }

    suspend fun lambdaNotInvoked() {
        //no warning should be present
        val fn1 = { Thread.sleep(3) }
    }
}