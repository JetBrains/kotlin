// "Make block type suspend" "true"
// WITH_RUNTIME

import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.startCoroutine

suspend fun <T> suspending(block: () -> T): T = suspendCoroutine { block.<caret>startCoroutine(it) }