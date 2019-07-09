// WITH_RUNTIME
// FIX: Merge call chain to 'withContext'

package kotlinx.coroutines

suspend fun test() {
    coroutineScope {
        <caret>async { 42 }.await()
    }
}