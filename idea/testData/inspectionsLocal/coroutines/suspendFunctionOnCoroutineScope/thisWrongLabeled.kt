// PROBLEM: none
// DISABLE-ERRORS

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

fun calcSomething() {}

class My {
    suspend fun <caret>CoroutineScope.foo() {
        this@My.async {
            calcSomething()
        }
    }
}