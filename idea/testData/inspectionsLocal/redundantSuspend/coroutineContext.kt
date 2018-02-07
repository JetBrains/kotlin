// PROBLEM: none
// WITH_RUNTIME

import kotlin.coroutines.experimental.coroutineContext

<caret>suspend fun test() {
    coroutineContext
}