// TARGET_BACKEND: JVM
// LANGUAGE: +JvmOptimizeImmediateRethrowForSuspend
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class TestException : RuntimeException()

var cont: Continuation<String>? = null

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { cont = it; COROUTINE_SUSPENDED }

@JvmInline
value class IC(val s: String)

suspend fun level3(): String = suspendHere() + "3"
suspend fun level2(): String = level3() + "2"
suspend fun icLevel(): IC = IC(level2())

val log = StringBuilder()

suspend fun withFinally(): String {
    try {
        return icLevel().s
    } finally {
        log.append("F")
    }
}

open class A {
    open suspend fun f(): String = level2() + "A"
}

interface Runner {
    suspend fun run(): String
}

inline fun inlineBuilder(crossinline c: suspend () -> String): suspend () -> String = { c() + "i" }

inline fun inlineRunner(crossinline c: suspend () -> String): Runner = object : Runner {
    override suspend fun run(): String = c() + "o"
}

inline fun nestedBuilder(crossinline c: suspend () -> String): suspend () -> String = inlineBuilder { inlineRunner { c() }.run() }

fun run(block: suspend () -> String, resumeWith: Result<String>): Result<String> {
    var r: Result<String>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { r = it })
    val c = cont!!
    cont = null
    c.resumeWith(resumeWith)
    return r!!
}

fun check(name: String, block: suspend () -> String): String? {
    val success = run(block, Result.success("x"))
    if (success.isFailure) return "FAIL $name: unexpected failure ${success.exceptionOrNull()}"
    val ex = TestException()
    val failure = run(block, Result.failure(ex))
    if (failure.exceptionOrNull() !== ex) return "FAIL $name: $failure"
    return null
}

// Resumes the first `failAt` suspensions with success, the next one with failure, and the rest with success.
fun runAt(block: suspend () -> String, failAt: Int, ex: Throwable): Result<String> {
    var r: Result<String>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { r = it })
    repeat(failAt) {
        val c = cont!!
        cont = null
        c.resume("x")
    }
    val c = cont!!
    cont = null
    c.resumeWithException(ex)
    while (r == null) {
        val next = cont!!
        cont = null
        next.resume("x")
    }
    return r!!
}

// States: 0, 1, 2 outside of try, 3 inside of try, 4 outside of try, 5, 6, 7 inside of try.
suspend fun manyStates(): String {
    var s = level2() + level2()
    s += try {
        level2()
    } catch (e: TestException) {
        "c1"
    }
    s += level2()
    try {
        s += level2() + level2() + level2()
    } catch (e: TestException) {
        s += "c2"
    }
    return s
}

fun checkManyStates(name: String, block: suspend () -> String, caughtAt: Set<Int>): String? {
    for (failAt in 0..6) {
        val ex = TestException()
        val result = runAt(block, failAt, ex)
        if (failAt in caughtAt) {
            if (result.isFailure) return "FAIL $name $failAt: not caught $result"
        } else {
            if (result.exceptionOrNull() !== ex) return "FAIL $name $failAt: $result"
        }
    }
    return null
}

fun box(): String {
    check("named") { level2() }?.let { return it }
    check("inline class") { icLevel().s }?.let { return it }
    check("crossinline lambda", inlineBuilder { level2() })?.let { return it }
    check("crossinline object") { inlineRunner { level2() }.run() }?.let { return it }
    check("nested crossinline", nestedBuilder { level2() })?.let { return it }
    check("open") { A().f() }?.let { return it }
    check("finally") { withFinally() }?.let { return it }
    if (log.toString() != "FF") return "FAIL finally: $log"

    checkManyStates("named", { manyStates() }, setOf(2, 4, 5, 6))?.let { return it }
    checkManyStates("lambda", {
        var s = level2() + level2()
        s += try {
            level2()
        } catch (e: TestException) {
            "c1"
        }
        s += level2()
        try {
            s += level2() + level2() + level2()
        } catch (e: TestException) {
            s += "c2"
        }
        s
    }, setOf(2, 4, 5, 6))?.let { return it }

    val ex = TestException()
    val caught = run({
        try {
            level2()
        } catch (e: TestException) {
            if (e === ex) "caught" else "wrong exception"
        }
    }, Result.failure(ex))
    if (caught.getOrNull() != "caught") return "FAIL catch: $caught"

    // Resume with failure in state 0 of a lambda.
    val ex0 = TestException()
    var r0: Result<String>? = null
    val lambda: suspend () -> String = { level2() }
    lambda.createCoroutine(Continuation(EmptyCoroutineContext) { r0 = it }).resumeWithException(ex0)
    if (r0?.exceptionOrNull() !== ex0) return "FAIL state 0: $r0"

    return "OK"
}
