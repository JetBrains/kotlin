// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

interface Scope

fun <T> doTest(
    coroutineContext: CoroutineContext,
    timeout: Long = 10000L,
    action: suspend Scope.() -> T
): T = TODO()

object Dispatcher : CoroutineContext {
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
        TODO()
    }

    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        TODO()
    }

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        TODO()
    }

}

class Cache {
    suspend fun getOrPutByString(str: String, put: suspend () -> Info): Info = TODO()
    suspend fun getOrPutById(id: ID, put: suspend () -> Info): Info = TODO()
    suspend fun removeById(id: ID, newValue: Info? = null) {}
}

class Info(val str: String)

typealias ID = String

private val Info.id get() = ""

inline fun expectAnyFailure(failureMessage: String? = null, action: () -> Unit) {
    expectFailure<Throwable>(failureMessage) {
        action()
    }
}

class LoggedErrors {
    var disabled = false

    val disabledTypes = mutableSetOf<String?>()
}

class AtomicReference<T>(var value: T) {
    fun get(): T = TODO()
}

object Expector {
    val currentErrors = AtomicReference<LoggedErrors?>(null)

    inline fun <T> disable(action: () -> T) {
        currentErrors.get()?.apply {
            val oldValue = disabled
            disabled = true
            try {
                action()
            } finally {
                disabled = oldValue
            }
        } ?: action()
    }
}

inline fun <reified E : Throwable> expectFailure(
    failureMessage: String? = null,
    noinline exceptionCheck: ((E) -> Unit)? = null,
    action: () -> Unit
) {
    var exceptionWasThrown = false
    try {
        Expector.disable {
            action()
        }
    } catch (ex: Throwable) {
        exceptionWasThrown = true
        // Exception is expected.
        assertTrue("'${ex::class}' was thrown.", ex is E)
        exceptionCheck?.invoke(ex as E)
    }

    if (!exceptionWasThrown) {
        fail("No exception was thrown.${failureMessage?.let { " $it" } ?: ""}")
    }
}

fun assertEquals(expected: Any?, actual: Any?) {}
fun fail(message: String) {
    error(message)
}

fun assertTrue(message: String, value: Boolean) {}

class Test {
    private val i1 = Info("1")
    private val i2 = Info("2")
    fun test() {
        doTest(Dispatcher) {
            val cache = Cache()

            cache.getOrPutByString(i1.str) { i1 }

            assertEquals(i1, cache.getByString(i1.str.toLowerCase()))
            assertEquals(i1, cache.getByString(i1.str.toUpperCase()))
            assertEquals(i1, cache.getById(i1.id))
            expectAnyFailure { cache.getByString(i2.str.toLowerCase()) }
            expectAnyFailure { cache.getByString(i2.str.toUpperCase()) }
            expectAnyFailure { cache.getById(i2.id) }

            cache.removeById(i2.id)

            assertEquals(i1, cache.getByString(i1.str.toLowerCase()))
            assertEquals(i1, cache.getByString(i1.str.toUpperCase()))
            assertEquals(i1, cache.getById(i1.id))
            expectAnyFailure { cache.getByString(i2.str.toLowerCase()) }
            expectAnyFailure { cache.getByString(i2.str.toUpperCase()) }
            expectAnyFailure { cache.getById(i2.id) }

            cache.removeById(i1.id)

            expectAnyFailure { cache.getByString(i1.str.toLowerCase()) }
            expectAnyFailure { cache.getByString(i1.str.toUpperCase()) }
            expectAnyFailure { cache.getById(i1.id) }
            expectAnyFailure { cache.getByString(i2.str.toLowerCase()) }
            expectAnyFailure { cache.getByString(i2.str.toUpperCase()) }
            expectAnyFailure { cache.getById(i2.id) }
        }
    }

    private suspend fun Cache.getByString(str: String) =
        getOrPutByString(str) { error("Not found $str") }

    private suspend fun Cache.getById(id: ID) =
        getOrPutById(id) { error("Not found $id") }
}

fun box(): String {
    // This is compiler sanity tests
    Test()
    return "OK"
}