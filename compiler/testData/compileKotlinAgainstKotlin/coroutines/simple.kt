// FILE: A.kt
// LANGUAGE_VERSION: 1.2

suspend fun dummy() = "OK"

// FILE: B.kt
// LANGUAGE_VERSION: 1.3
import kotlin.coroutines.experimental.*

fun box(): String {
    return dummy(object : Continuation<String> {
        override val context = EmptyCoroutineContext
        override fun resume(value: String) {
        }
        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    }) as String
}
