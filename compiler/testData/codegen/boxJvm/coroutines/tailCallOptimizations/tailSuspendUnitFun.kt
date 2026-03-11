// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun empty() = TailCallOptimizationChecker.saveStackTrace()
suspend fun withoutReturn() { empty() }
suspend fun withReturn() { return empty() }
suspend fun notTailCall() { empty(); empty() }
suspend fun lambdaAsParameterAfterUnsafeCast(c: suspend ()->Unit) { c() }
suspend fun lambdaAsParameter(c: suspend ()->Unit) { c() }
suspend fun lambdaAsParameterNotTailCall(c: suspend ()->Unit) { c(); c() }
suspend fun lambdaAsParameterReturn(c: suspend ()->Unit) { return c() }

suspend fun returnsInt() = 42.also { TailCallOptimizationChecker.saveStackTrace() }
suspend fun callsIntNoTailCall() { returnsInt() }
suspend inline fun inlineNotTailCall() { returnsInt() }
suspend fun callsInlineNotTailCall() { inlineNotTailCall() }
suspend fun multipleExitPoints(b: Boolean) { if (b) empty() else withoutReturn() }
suspend fun multipleExitPointsNotTailCall(b: Boolean) { if (b) empty() else returnsInt() }

fun ordinary() = 1
inline fun ordinaryInline() { ordinary() }
suspend fun multipleExitPointsWithOrdinaryInline(b: Boolean) { if (b) empty() else ordinaryInline() }

suspend fun multipleExitPointsWhen(i: Int) {
    when(i) {
        1 -> empty()
        2 -> withReturn()
        3 -> withoutReturn()
        else -> lambdaAsParameter {}
    }
}

suspend fun <T> genericUnsafe() = (100 as T).also { TailCallOptimizationChecker.saveStackTrace() }
suspend fun useGenericUnsafe() {
    genericUnsafe<Unit>()
}
suspend fun <T> generic() = (Unit as T).also { TailCallOptimizationChecker.saveStackTrace() }
suspend fun useGenericReturningUnit() {
    generic<Unit>()
}

class Generic<T> {
    suspend fun foo() = generic<T>()
}
suspend fun useGenericClass(g: Generic<Unit>) {
    g.foo()
}

suspend fun <T> genericInferType(c: () -> T) = c().also { TailCallOptimizationChecker.saveStackTrace() }
suspend fun useGenericInferType() {
    genericInferType {}
}

suspend fun nullableUnit(): Unit? = null.also { TailCallOptimizationChecker.saveStackTrace() }
suspend fun useNullableUnit() {
    nullableUnit()
}

suspend fun useRunRunRunRunRun() {
    run {
        run {
            run {
                run {
                    run {
                        empty()
                    }
                }
            }
        }
    }
}


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        withoutReturn()
        TailCallOptimizationChecker.checkNoStateMachineIn("withoutReturn")

        withReturn()
        TailCallOptimizationChecker.checkNoStateMachineIn("withReturn")

        notTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("notTailCall")

        // although such example is not safe for tail-call optimization, we ignore it as:
        // - if we cannot trust lambda types, all "lambdaAsParameter" and imilar examples will become unoptimized
        // - with a presence of unsafe casts, NO tail-call optimizations are safe, because one can cast e.g Continuation<Int> to
        //   Continuation<String> and call resumeWith() with an unexpected type.
        val lambdaInt: suspend ()->Int =  { TailCallOptimizationChecker.saveStackTrace(); 10 }
        lambdaAsParameterAfterUnsafeCast(lambdaInt as suspend ()->Unit)
        TailCallOptimizationChecker.checkNoStateMachineIn("lambdaAsParameterAfterUnsafeCast")

        lambdaAsParameter { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkNoStateMachineIn("lambdaAsParameter")

        lambdaAsParameterNotTailCall { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkStateMachineIn("lambdaAsParameterNotTailCall")

        lambdaAsParameterReturn { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkNoStateMachineIn("lambdaAsParameterReturn")

        callsIntNoTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("callsIntNoTailCall")

        callsInlineNotTailCall()
        TailCallOptimizationChecker.checkStateMachineIn("callsInlineNotTailCall")

        multipleExitPoints(false)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPoints")

        multipleExitPointsNotTailCall(false)
        TailCallOptimizationChecker.checkStateMachineIn("multipleExitPointsNotTailCall")

        multipleExitPointsWithOrdinaryInline(true)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPointsWithOrdinaryInline")

        multipleExitPointsWhen(1)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPointsWhen")

        // similarly to lambdas, there is no exclusion of TCO for "unsafe" generic calls
        useGenericUnsafe()
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericUnsafe")

        useGenericReturningUnit()
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericReturningUnit")

        useGenericClass(Generic())
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericClass")

        useGenericInferType()
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericInferType")

        useNullableUnit()
        TailCallOptimizationChecker.checkStateMachineIn("useNullableUnit")

        useRunRunRunRunRun()
        TailCallOptimizationChecker.checkNoStateMachineIn("useRunRunRunRunRun")
    }
    return "OK"
}
