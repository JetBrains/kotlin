// "Fix experimental coroutines usage" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// WITH_RUNTIME

import kotlin.coroutines.experimental.Continuation

fun test(con: Continuation<Int>) {
    con.<caret>resume(12)
}