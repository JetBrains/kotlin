import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun doSomething() {}

fun foo() {
    val scope = object : CoroutineScope {
        suspend fun foo() {
            <caret>async { doSomething() }
        }
    }
}