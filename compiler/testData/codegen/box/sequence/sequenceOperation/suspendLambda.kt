// WITH_STDLIB
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private class FakeSemaphore {
    var acquired = 0
    var released = 0

    suspend fun acquire() {
        acquired++
    }

    fun release() {
        released++
    }
}

private class FakeScope {
    var launched = 0

    fun launch(block: suspend () -> Unit) {
        launched++
    }
}

private suspend fun process(
    semaphore: FakeSemaphore,
    scope: FakeScope
): Int {
    val values = sequence {
        yield(1)
        yield(2)
        yield(3)
    }

    values.forEach { value ->
        semaphore.acquire()
        try {
            scope.launch {
                check(value > 0)
            }
        } finally {
            semaphore.release()
        }
    }

    return values.count()
}

private fun <T> runBlocking(block: suspend () -> T): T {
    var result: Result<T>? = null

    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(value: Result<T>) {
            result = value
        }
    })

    return result!!.getOrThrow()
}

fun box(): String {
    val semaphore = FakeSemaphore()
    val scope = FakeScope()

    val count = runBlocking {
        process(semaphore, scope)
    }

    return if (
        count == 3 &&
        scope.launched == 3 &&
        semaphore.acquired == 3 &&
        semaphore.released == 3
    ) {
        "OK"
    } else {
        "FAIL"
    }
}
