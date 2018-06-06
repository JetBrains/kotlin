// "Make block type suspend" "true"
// WITH_RUNTIME

import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.coroutines.experimental.startCoroutine

suspend fun <T> suspending(block: () -> T): T = suspendCoroutine { block.<caret>startCoroutine(it) }