// WITH_RUNTIME
// FIX: Replace negated 'isEmpty' with 'isNotEmpty'

fun test() {
    val list = listOf(1,2,3)
    if (!list.<caret>isEmpty()) {
    }
}