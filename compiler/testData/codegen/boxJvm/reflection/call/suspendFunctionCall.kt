// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.coroutines.*
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class SuspendSubject {
    suspend fun noArg(): String = "no-arg"
    suspend fun oneArg(x: Int): String = "arg:$x"
    suspend fun withDefault(x: Int, y: String = "default"): String = "$x:$y"
    suspend fun nullableReturn(b: Boolean): String? = if (b) "yes" else null
    fun regularFun(x: Int): Int = x * 2
}

// Helper to run a suspend block synchronously
private fun <T> runSync(block: suspend () -> T): T {
    var result: T? = null
    var error: Throwable? = null
    block.startCoroutine(object : Continuation<T> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(outcome: Result<T>) {
            outcome.fold({ result = it }, { error = it })
        }
    })
    if (error != null) throw error!!
    @Suppress("UNCHECKED_CAST")
    return result as T
}

fun box(): String {
    val subj = SuspendSubject()
    val fns = SuspendSubject::class.memberFunctions.associateBy { it.name }

    // isSuspend flag
    assertTrue(fns["noArg"]!!.isSuspend)
    assertTrue(fns["oneArg"]!!.isSuspend)
    assertTrue(fns["withDefault"]!!.isSuspend)
    assertTrue(fns["nullableReturn"]!!.isSuspend)
    assertFalse(fns["regularFun"]!!.isSuspend)

    // callSuspend via callSuspendBy
    val noArgFn = fns["noArg"]!!
    val noArgResult = runSync { noArgFn.callSuspendBy(mapOf(noArgFn.instanceParameter!! to subj)) }
    assertEquals("no-arg", noArgResult)

    val oneArgFn = fns["oneArg"]!!
    val oneArgResult = runSync {
        oneArgFn.callSuspendBy(mapOf(
            oneArgFn.instanceParameter!! to subj,
            oneArgFn.valueParameters[0] to 42
        ))
    }
    assertEquals("arg:42", oneArgResult)

    // callSuspend with defaults
    val withDefaultFn = fns["withDefault"]!!
    val withDefaultResult = runSync {
        withDefaultFn.callSuspendBy(mapOf(
            withDefaultFn.instanceParameter!! to subj,
            withDefaultFn.valueParameters[0] to 7
            // y uses its default
        ))
    }
    assertEquals("7:default", withDefaultResult)

    // callSuspend with explicit named arg overriding default
    val withBothResult = runSync {
        withDefaultFn.callSuspendBy(mapOf(
            withDefaultFn.instanceParameter!! to subj,
            withDefaultFn.valueParameters[0] to 3,
            withDefaultFn.valueParameters[1] to "override"
        ))
    }
    assertEquals("3:override", withBothResult)

    // nullable return
    val nullableFn = fns["nullableReturn"]!!
    val trueResult = runSync {
        nullableFn.callSuspendBy(mapOf(
            nullableFn.instanceParameter!! to subj,
            nullableFn.valueParameters[0] to true
        ))
    }
    assertEquals("yes", trueResult)
    val falseResult = runSync {
        nullableFn.callSuspendBy(mapOf(
            nullableFn.instanceParameter!! to subj,
            nullableFn.valueParameters[0] to false
        ))
    }
    assertNull(falseResult)

    return "OK"
}
