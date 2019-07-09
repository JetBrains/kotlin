// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(scope: CoroutineScope) {
    scope.<caret>async() { 42 }.await()
}