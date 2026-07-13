// KT-66098: ClassCastException
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JS_IR_ES6
// ^^^^
// org.jetbrains.kotlin.js.engine.ScriptExecutionException: ERROR:
// AssertionError: Expected <159>, actual <[object Generator]>.
// KT-82349: ClassCastException

// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.3,2.4
// ^^^ KT-78040 is available in 2.4.20-Beta2

import kotlin.test.*

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

fun box(): String {
    val ref = Foo(42)::bar
    assertEquals(159, (ref as Function2<Int, Continuation<Int>, Any?>)(117, EmptyContinuation))
    return "OK"
}
