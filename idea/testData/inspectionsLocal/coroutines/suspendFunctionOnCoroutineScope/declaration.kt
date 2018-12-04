import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

suspend fun CoroutineScope.foo() {
    val deferred = <caret>async {
        calcSomething()
    }
}