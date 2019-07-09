// FIX: Wrap function body with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

abstract class MyCoroutineScope : CoroutineScope {
    suspend fun <caret>foo() = this.async { calcSomething() }
}