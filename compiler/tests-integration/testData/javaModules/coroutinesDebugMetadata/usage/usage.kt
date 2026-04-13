@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package some.module.withsome.packages

import java.lang.RuntimeException
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.jvm.internal.BaseContinuationImpl

suspend fun dummy() {}

class Test {
    suspend fun getStackTraceElement(): StackTraceElement {
        dummy() // to force state-machine generation
        return suspendCoroutineUninterceptedOrReturn<StackTraceElement> {
            (it as BaseContinuationImpl).getStackTraceElement()
        }
    }
}

suspend fun main() {
    println(Test().getStackTraceElement().className)
}
