// WITH_RUNTIME
// PROBLEM: none

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext) {
    // Does not work yet (1.3.11)
    GlobalScope.<caret>async(ctx) { 42 }.await()
}