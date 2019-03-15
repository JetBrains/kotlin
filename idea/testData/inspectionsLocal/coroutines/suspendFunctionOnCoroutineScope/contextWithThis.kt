// FIX: Wrap call with 'coroutineScope { ... }'

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineContext

fun use(context: CoroutineContext) {}

suspend fun CoroutineScope.foo() {
    use(this.<caret>coroutineContext)
}