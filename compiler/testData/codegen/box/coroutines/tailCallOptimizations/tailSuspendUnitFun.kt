// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun empty() = TailCallOptimizationChecker.saveStackTrace()
suspend fun withoutReturn() { empty() }
suspend fun withReturn() { return empty() }
suspend fun notTailCall() { empty(); empty() }
suspend fun lambdaAsParameter(c: suspend ()->Unit) { c() }
suspend fun lambdaAsParameterNotTailCall(c: suspend ()->Unit) { c(); c() }
suspend fun lambdaAsParameterReturn(c: suspend ()->Unit) { return c() }
suspend fun returnsInt() = 42.also { TailCallOptimizationChecker.saveStackTrace() }
// This should not be tail-call, since the caller should push Unit.INSTANCE on stack
suspend fun callsIntTailCall() { returnsInt() }
suspend fun multipleExitPoints(b: Boolean) { if (b) empty() else withoutReturn() }
suspend fun multipleExitPointsTailCall(b: Boolean) { if (b) empty() else returnsInt() }

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

        lambdaAsParameter { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkNoStateMachineIn("lambdaAsParameter")

        lambdaAsParameterNotTailCall { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkStateMachineIn("lambdaAsParameterNotTailCall")

        lambdaAsParameterReturn { TailCallOptimizationChecker.saveStackTrace() }
        TailCallOptimizationChecker.checkNoStateMachineIn("lambdaAsParameterReturn")

        callsIntTailCall()
        TailCallOptimizationChecker.checkNoStateMachineIn("callsIntTailCall")

        multipleExitPoints(false)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPoints")

        multipleExitPointsTailCall(false)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPointsTailCall")

        multipleExitPointsWithOrdinaryInline(true)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPointsWithOrdinaryInline")

        multipleExitPointsWhen(1)
        TailCallOptimizationChecker.checkNoStateMachineIn("multipleExitPointsWhen")

        useGenericReturningUnit()
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericReturningUnit")

        useGenericClass(Generic())
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericClass")

        useGenericInferType()
        TailCallOptimizationChecker.checkNoStateMachineIn("useGenericInferType")

        useNullableUnit()
        TailCallOptimizationChecker.checkNoStateMachineIn("useNullableUnit")

        useRunRunRunRunRun()
        TailCallOptimizationChecker.checkNoStateMachineIn("useRunRunRunRunRun")
    }
    return "OK"
}
