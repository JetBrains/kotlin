// KT-66098: ClassCastException
// KT-78040 K/Wasm: consider making implementations (!) of suspend lambdas a subtype of FunctionX interfaces

// WITH_STDLIB
// WITH_COROUTINES
// WITH_REFLECT

// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ KT-35479 KT-82349: ClassCastException

// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3,2.4
// ^^^ K/Native didn't generate KSuspendFunctionN <: KFunction{N + 1}
// fixed after KT-78040 (AddFunctionSupertypeToSuspendFunctionLowering was modified
// and moved into backend.common to we used by Wasm as well)

import kotlin.reflect.KFunction2
import kotlin.reflect.KSuspendFunction1
import kotlin.test.*

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

class Foo(val x: Int) {
    suspend fun bar(y: Int) = foo(y) + x
}

suspend fun foo(x: Int) = x

fun builder(c: suspend () -> Int): Int {
    var res = 0
    c.startCoroutine(handleResultContinuation {
        res = it
    })

    return res
}

fun box(): String {
    val ref = Foo(42)::bar

    assertEquals(159, (ref as KFunction2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))
    assertEquals(159, (ref as Function2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))
    assertTrue(ref is KSuspendFunction1<Int, Any?>)
    assertTrue(ref is SuspendFunction1<Int, Any?>)
    assertEquals(159, builder {
        (ref as SuspendFunction1<Int, Int>)(117)
    })
    assertEquals(159, builder {
        (ref as KSuspendFunction1<Int, Int>)(117)
    })
    assertEquals(159, builder {
        (ref as suspend (Int) -> Int)(117)
    })

    val ref1 = suspend { x: Int -> x }
    assertTrue(ref1 !is KSuspendFunction1<Int, Any?>)
//    assertTrue(ref1 !is KFunction2<Int, Continuation<Int>, Any?>) // type erasure
    assertTrue(ref1 !is KFunction2<*, *, *>)
    assertEquals(117, (ref1 as Function2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))
    assertTrue(ref1 is SuspendFunction1<Int, Any?>)
    assertTrue(ref1 is Function2<Int, Continuation<Int>, Any?>)
    assertEquals(117, builder {
        (ref1 as SuspendFunction1<Int, Int>)(117)
    })
    assertEquals(117, builder {
        (ref1 as suspend (Int) -> Int)(117)
    })

    return "OK"
}
