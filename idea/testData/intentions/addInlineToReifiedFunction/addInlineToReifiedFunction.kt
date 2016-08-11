// INTENTION_TEXT: Add Inline to reified function
// ERROR: Only type parameters of inline functions can be reified
// SKIP_ERRORS_AFTER

fun <<caret>reified T> fn() {
}