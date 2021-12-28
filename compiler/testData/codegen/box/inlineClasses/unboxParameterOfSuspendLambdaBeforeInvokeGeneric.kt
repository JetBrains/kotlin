// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// WITH_COROUTINES
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxAny<T>(val value: T) {
    val intValue: Int get() = value as Int
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxInt<T: Int>(val value: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxLong<T: Long>(val value: T)

class EmptyContinuation<T> : Continuation<T> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {}
}

suspend fun <T> foo(block: suspend (BoxAny<T>) -> Unit) {
    block(BoxAny<T>(1 as T))
    block.startCoroutineUninterceptedOrReturn(BoxAny<T>(1 as T), EmptyContinuation())
}

suspend fun <T> fooReceiver(block: suspend BoxAny<T>.() -> Unit) {
    BoxAny<T>(1 as T).block()
    block.startCoroutineUninterceptedOrReturn(BoxAny<T>(1 as T), EmptyContinuation())
}

suspend fun <T: Int> bar(block: suspend (BoxInt<T>) -> Unit) {
    block(BoxInt<T>(2 as T))
    block.startCoroutineUninterceptedOrReturn(BoxInt<T>(2 as T), EmptyContinuation())
}

suspend fun <T: Int> barReceiver(block: suspend BoxInt<T>.() -> Unit) {
    BoxInt<T>(2 as T).block()
    block.startCoroutineUninterceptedOrReturn(BoxInt<T>(2 as T), EmptyContinuation())
}

suspend fun <T: Long> baz(block: suspend (BoxLong<T>) -> Unit) {
    block(BoxLong<T>(3L as T))
    block.startCoroutineUninterceptedOrReturn(BoxLong<T>(3L as T), EmptyContinuation())
}

suspend fun <T: Long> bazReceiver(block: suspend BoxLong<T>.() -> Unit) {
    BoxLong<T>(3L as T).block()
    block.startCoroutineUninterceptedOrReturn(BoxLong<T>(3L as T), EmptyContinuation())
}

suspend fun <T> BoxAny<T>.extension(block: suspend BoxAny<T>.() -> Unit) {
    this.block()
    block()

    block.startCoroutineUninterceptedOrReturn(this, EmptyContinuation())
}

suspend fun <T: Int> BoxInt<T>.extension(block: suspend BoxInt<T>.() -> Unit) {
    this.block()
    block()

    block.startCoroutineUninterceptedOrReturn(this, EmptyContinuation())
}

suspend fun <T: Long> BoxLong<T>.extension(block: suspend BoxLong<T>.() -> Unit) {
    this.block()
    block()

    block.startCoroutineUninterceptedOrReturn(this, EmptyContinuation())
}

fun runBlocking(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<Unit>) {
            (block as Function1<Continuation<Unit>, Any?>)(this)
        }
    })
}

fun box(): String {
    var result = 0
    runBlocking {
        foo<Int> { boxAny ->
            result += boxAny.intValue
        }
        fooReceiver<Int> {
            result += this.intValue
        }

        bar<Int> { boxInt ->
            result += boxInt.value
        }
        barReceiver<Int> {
            result += value
        }

        baz<Long> { boxLong ->
            result += boxLong.value.toInt()
        }
        bazReceiver<Long> {
            result += this.value.toInt()
        }

        val b = BoxAny(4)
        b.extension {
            result += intValue
        }

        val bInt = BoxInt(5)
        BoxInt(5).extension {
            result += value + bInt.value
        }

        BoxLong(6).extension {
            result += value.toInt()
        }
    }

    return if (result == 168) "OK" else "Error: $result"
}