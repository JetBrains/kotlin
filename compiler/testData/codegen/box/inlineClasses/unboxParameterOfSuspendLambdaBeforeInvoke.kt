// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// WITH_COROUTINES
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxAny(val value: Any?) {
    val intValue: Int get() = value as Int
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxInt(val value: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class BoxLong(val value: Long)

class EmptyContinuation<T> : Continuation<T> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {}
}

suspend fun foo(block: suspend (BoxAny) -> Unit) {
    block(BoxAny(1))
    block.startCoroutineUninterceptedOrReturn(BoxAny(1), EmptyContinuation())
}

suspend fun fooReceiver(block: suspend BoxAny.() -> Unit) {
    BoxAny(1).block()
    block.startCoroutineUninterceptedOrReturn(BoxAny(1), EmptyContinuation())
}

suspend fun bar(block: suspend (BoxInt) -> Unit) {
    block(BoxInt(2))
    block.startCoroutineUninterceptedOrReturn(BoxInt(2), EmptyContinuation())
}

suspend fun barReceiver(block: suspend BoxInt.() -> Unit) {
    BoxInt(2).block()
    block.startCoroutineUninterceptedOrReturn(BoxInt(2), EmptyContinuation())
}

suspend fun baz(block: suspend (BoxLong) -> Unit) {
    block(BoxLong(3))
    block.startCoroutineUninterceptedOrReturn(BoxLong(3), EmptyContinuation())
}

suspend fun bazReceiver(block: suspend BoxLong.() -> Unit) {
    BoxLong(3).block()
    block.startCoroutineUninterceptedOrReturn(BoxLong(3), EmptyContinuation())
}

suspend fun BoxAny.extension(block: suspend BoxAny.() -> Unit) {
    this.block()
    block()

    block.startCoroutineUninterceptedOrReturn(this, EmptyContinuation())
}

suspend fun BoxInt.extension(block: suspend BoxInt.() -> Unit) {
    this.block()
    block()

    block.startCoroutineUninterceptedOrReturn(this, EmptyContinuation())
}

suspend fun BoxLong.extension(block: suspend BoxLong.() -> Unit) {
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
        foo { boxAny ->
            result += boxAny.intValue
        }
        fooReceiver {
            result += this.intValue
        }

        bar { boxInt ->
            result += boxInt.value
        }
        barReceiver {
            result += value
        }

        baz { boxLong ->
            result += boxLong.value.toInt()
        }
        bazReceiver {
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