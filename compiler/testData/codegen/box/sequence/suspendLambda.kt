// WITH_STDLIB

private class FakeSemaphore {
    suspend fun acquire() {}

    fun release() {}
}

private class FakeScope {
    var launched = 0

    fun launch(block: suspend () -> Unit) {
        launched++
    }
}

fun box(): String {
    val semaphore = FakeSemaphore()
    val scope = FakeScope()

    val result = sequenceOf(1, 2, 3)
        .map { value ->
            scope.launch {
                semaphore.acquire()
                try {
                    check(value > 0)
                } finally {
                    semaphore.release()
                }
            }

            value * 2
        }
        .toList()

    return if (
        result == listOf(2, 4, 6) &&
        scope.launched == 3
    ) {
        "OK"
    } else {
        "FAIL"
    }
}
