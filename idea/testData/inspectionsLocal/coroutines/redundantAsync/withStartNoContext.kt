// PROBLEM: none
// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    <caret>async(start = CoroutineStart.LAZY) { 42 }.await()
}