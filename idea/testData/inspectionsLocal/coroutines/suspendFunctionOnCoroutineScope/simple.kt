// FIX: Remove receiver & wrap with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() {}

suspend fun <caret>CoroutineScope.foo() {
    async {
        calcSomething()
    }
}