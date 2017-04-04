import kotlin.test.*

import kotlin.coroutines.experimental.*

class BadClass {
    override fun equals(other: Any?): Boolean = error("equals")
    override fun hashCode(): Int = error("hashCode")
    override fun toString(): String = error("toString")
}

var counter = 0

// tail-suspend function via suspendCoroutine (test SafeContinuation)
suspend fun getBadClassViaSuspend(): BadClass = suspendCoroutine { cont ->
    counter++
    cont.resume(BadClass())
}

// state machine
suspend fun checkBadClassTwice() {
    assertTrue(getBadClassViaSuspend() is BadClass)
    assertTrue(getBadClassViaSuspend() is BadClass)
}

fun <T> suspend(block: suspend () -> T) = block

fun box() {
    val bad = suspend {
        checkBadClassTwice()
        getBadClassViaSuspend()
    }
    var result: BadClass? = null
    bad.startCoroutine(object : Continuation<BadClass> {
        override val context: CoroutineContext = EmptyCoroutineContext

        override fun resume(value: BadClass) {
            assertTrue(result == null)
            result = value
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
    assertTrue(result is BadClass)
    assertEquals(3, counter)
}
