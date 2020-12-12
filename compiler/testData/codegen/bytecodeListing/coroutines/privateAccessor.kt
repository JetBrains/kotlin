// WITH_RUNTIME
import kotlin.coroutines.*

private fun foo() {}

private suspend fun bar() = suspendCoroutine<Unit> {
    foo()
}
