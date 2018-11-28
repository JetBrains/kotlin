// PROBLEM: none
// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    <caret>async(parent = null) { 42 }.await()
}