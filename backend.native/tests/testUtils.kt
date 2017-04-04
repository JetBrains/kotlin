package kotlin.test

annotation class Test()

class TestFailedException(val msg:String):RuntimeException(msg)

class Assert {
    companion object {
        fun <T> assertEquals(a:T, b:T, msg:String? = null) { if (a != b) throw TestFailedException(msg ?: "") }
    }
}

fun <T> assertEquals(a:T, b:T, msg:String? = null) = Assert.assertEquals(a, b, msg)

/** Asserts that the expression is `true` with an optional [message]. */
fun assertTrue(actual: Boolean, message: String? = null) {
    if (actual != true) {
        throw TestFailedException(message ?: "Expected value to be true.")
    }
}

/** Asserts that the expression is `false` with an optional [message]. */
fun assertFalse(actual: Boolean, message: String? = null) {
    assertTrue(!actual, message ?: "Expected value to be false.")
}

/** Fails the current test with the specified [message]. */
fun fail(message: String? = null): Nothing = throw TestFailedException(message ?: "Expected value to be true.")


/** Asserts that given function [block] returns the given [expected] value. */
fun <T> expect(expected: T, block: () -> T) {
    assertEquals(expected, block())
}

/** Asserts that given function [block] returns the given [expected] value and use the given [message] if it fails. */
fun <T> expect(expected: T, message: String?, block: () -> T) {
    assertEquals(expected, block(), message)
}

/** Asserts that given function [block] fails by throwing an exception. */
fun assertFails(block: () -> Unit): Throwable = assertFails(null, block)

/** Asserts that given function [block] fails by throwing an exception. */
fun assertFails(message: String?, block: () -> Unit): Throwable {
    try {
        block()
    } catch (e: Throwable) {
        return e
    }
    fail(message + ". Expected an exception to be thrown, but was completed successfully.")
}

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

/** Asserts that the given [block] returns `true`. */
fun assertTrue(message: String? = null, block: () -> Boolean): Unit = assertTrue(block(), message)

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    assertFalse(illegal == actual, message)
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

/** Asserts that the [actual] value is not `null`, with an optional [message]. */
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    assertTrue(actual != null, message)
    return actual!!
}

/** Asserts that the [actual] value is not `null`, with an optional [message] and a function [block] to process the not-null value. */
fun <T : Any, R> assertNotNull(actual: T?, message: String? = null, block: (T) -> R) {
    assertTrue(actual != null, message)
    if (actual != null) {
        block(actual)
    }
}

/** Asserts that the [actual] value is `null`, with an optional [message]. */
fun assertNull(actual: Any?, message: String? = null) {
    assertTrue(actual == null, message)
}

