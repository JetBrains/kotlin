// WITH_RUNTIME
// FIX: Merge call chain to 'withContext'

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    <caret>async(ctx) { 42 }.await()
}