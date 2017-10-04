// "Make block type suspend" "true"
// WITH_RUNTIME
// DISABLE-ERRORS

import kotlin.coroutines.experimental.suspendCoroutine

suspend fun <T> suspending(block: () -> T): T = suspendCoroutine { block.<caret>startCoroutine(it) }