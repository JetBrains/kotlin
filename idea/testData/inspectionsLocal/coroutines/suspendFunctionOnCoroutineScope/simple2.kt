// FIX: Remove receiver & wrap with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() {}

class Bar {
    fun bar() {}
}

suspend fun <caret>CoroutineScope.foo() {
    Bar().bar()
    async {
        calcSomething()
    }
    Bar().bar()
}