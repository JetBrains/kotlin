// WITH_RUNTIME
// FIX: Merge call chain to 'withContext(DefaultDispatcher)'

package kotlinx.coroutines

suspend fun test() {
    <caret>async { 42 }.await()
}