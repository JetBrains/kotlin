import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

object MyCoroutineScope : CoroutineScope {
    suspend fun <caret>foo() = async { calcSomething() }
}