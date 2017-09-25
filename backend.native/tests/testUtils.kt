package kotlin.test

// TODO: Remove it when such method from library is available.
/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
inline fun <reified T : Throwable> assertFailsWith(message: String? = null, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (e is T) {
            @Suppress("UNCHECKED_CAST")
            return e
        }

        @Suppress("INVISIBLE_MEMBER")
        fail(message + ". Expected an exception of type //TODO: add type!// to be thrown, but was $e")
    }

    @Suppress("INVISIBLE_MEMBER")
    fail(message + ". Expected an exception of type //TODO: add type!//  to be thrown, but was completed successfully.")
}

@Suppress("UNUSED_PARAMETER")
public fun assertTypeEquals(expected: Any?, actual: Any?) {
    //TODO: find analogue
    //assertEquals(expected?.javaClass, actual?.javaClass)
}

fun <T> Iterable<T>.assertSorted(isInOrder: (T, T) -> Boolean): Unit { this.iterator().assertSorted(isInOrder) }
fun <T> Iterator<T>.assertSorted(isInOrder: (T, T) -> Boolean) {
    if (!hasNext()) return
    var index = 0
    var prev = next()
    while (hasNext()) {
        index += 1
        val next = next()
        assertTrue(isInOrder(prev, next), "Not in order at position $index, element[${index-1}]: $prev, element[$index]: $next")
        prev = next
    }
    return
}
