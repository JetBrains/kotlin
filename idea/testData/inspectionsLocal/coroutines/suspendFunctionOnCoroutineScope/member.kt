// FIX: Move to companion object

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() = 42

abstract class MyCoroutineScope : CoroutineScope {
    suspend fun <caret>foo() = async { calcSomething() }
}