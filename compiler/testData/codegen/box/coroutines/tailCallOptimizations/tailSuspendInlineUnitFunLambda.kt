// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun empty() = TailCallOptimizationChecker.saveStackTrace()
suspend fun returnsInt() = 42.also { TailCallOptimizationChecker.saveStackTrace() }

suspend inline fun lambdaAsParameterInline(c: suspend ()->Unit) { c() }
suspend inline fun lambdaAsParameterCrossInline(crossinline c: suspend ()->Unit) { c() }
suspend inline fun lambdaAsParameterNoInline(noinline c: suspend ()->Unit) { c() }
suspend inline fun lambdaAsParameterInline2(c: suspend ()->Unit) { lambdaAsParameterInline(c) }
suspend fun useLambdaAsParameterInline() { lambdaAsParameterInline { empty() } }
suspend fun useLambdaAsParameterInlineNotTailCall() { lambdaAsParameterInline { returnsInt() } }
suspend fun useLambdaAsParameterCrossInline() { lambdaAsParameterCrossInline { empty() } }
suspend fun useLambdaAsParameterCrossInlineNotTailCall() { lambdaAsParameterCrossInline { returnsInt() } }
suspend fun useLambdaAsParameterNoInline() { lambdaAsParameterNoInline { empty() } }
suspend fun useLambdaAsParameterNoInlineWithReturnsInt() { lambdaAsParameterNoInline { returnsInt() } }
suspend fun useLambdaAsParameterInline2() { lambdaAsParameterInline2 { empty() } }
suspend fun useLambdaAsParameterInline2NotTailCall() { lambdaAsParameterInline2 { returnsInt() } }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        useLambdaAsParameterInline()
        TailCallOptimizationChecker.checkNoStateMachineIn("useLambdaAsParameterInline")

        useLambdaAsParameterInlineNotTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("useLambdaAsParameterInlineNotTailCall")

        useLambdaAsParameterCrossInline()
        TailCallOptimizationChecker.checkNoStateMachineIn("useLambdaAsParameterCrossInline")

        useLambdaAsParameterCrossInlineNotTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("useLambdaAsParameterCrossInlineNotTailCall")

        useLambdaAsParameterNoInline()
        TailCallOptimizationChecker.checkNoStateMachineIn("useLambdaAsParameterNoInline")

        // despite returnsInt() call, useLambdaAsParameterNoInlineWithReturnsInt shall not have SM
        //  (as it will be in useLambdaAsParameterNoInlineWithReturnsInt$2.invokeSuspend())
        useLambdaAsParameterNoInlineWithReturnsInt()
        TailCallOptimizationChecker.checkNoStateMachineIn("useLambdaAsParameterNoInlineWithReturnsInt")

        useLambdaAsParameterInline2()
        TailCallOptimizationChecker.checkNoStateMachineIn("useLambdaAsParameterInline2")

        useLambdaAsParameterInline2NotTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("useLambdaAsParameterInline2NotTailCall")
    }
    return "OK"
}
