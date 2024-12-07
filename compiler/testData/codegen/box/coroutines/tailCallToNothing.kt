// WITH_STDLIB
// WITH_COROUTINES
// SKIP_MANGLE_VERIFICATION

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Success : RuntimeException()

suspend fun suspendThenThrow(): Nothing {
    suspendCoroutineUninterceptedOrReturn<Unit> {
        it.resume(Unit)
        COROUTINE_SUSPENDED
    }
    throw Success()
}

suspend fun foo(x: Int = 1): Nothing = suspendThenThrow()

interface I {
    suspend fun bar(): Nothing
}

class C : I by (object : I {
    override suspend fun bar(): Nothing = suspendThenThrow()
})

var result = ""

fun box(): String {
    var counter = 0
    suspend {
        // 1. foo$default is a bridge to foo, should not throw after foo returns
        try { foo() } catch (e: Success) { counter++ }
        // 2. C.bar is a delegation to another method, so same
        try { C().bar() } catch (e: Success) { counter++ }
        // 3. object.foo$default is the same as 1 but in a local object
        try {
            object {
                suspend fun foo(x: Int = 1): Nothing = suspendThenThrow()
            }.foo()
        } catch (e: Success) { counter++ }
        // 4. this point should be reached exactly once
        counter++
    }.startCoroutine(EmptyContinuation)
    return if (counter == 4) "OK" else "counter incremented $counter times"
}
