// WITH_RUNTIME
// PROBLEM: none

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext, scope: CoroutineScope) {
    // Does not work yet (1.3.11)
    scope.<caret>async(ctx) { 42 }.await()
}