// WITH_RUNTIME
// PROBLEM: none

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    coroutineScope {
        <caret>async(ctx, CoroutineStart.LAZY) { 42 }.await()
    }
}