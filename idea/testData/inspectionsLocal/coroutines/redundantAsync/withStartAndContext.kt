// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    <caret>async(ctx, CoroutineStart.LAZY) { 42 }.await()
}