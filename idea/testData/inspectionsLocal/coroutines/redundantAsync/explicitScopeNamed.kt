// WITH_RUNTIME

package kotlinx.coroutines

suspend fun test(ctx: CoroutineContext, scope: CoroutineScope) {
    scope.<caret>async(context = ctx) { 42 }.await()
}