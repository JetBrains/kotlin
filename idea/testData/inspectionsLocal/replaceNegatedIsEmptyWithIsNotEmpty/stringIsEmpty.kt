// PROBLEM: Replace negated 'isEmpty' with 'isNotEmpty'
// FIX: Replace negated 'isEmpty' with 'isNotEmpty'
// WITH_RUNTIME
fun test(s: String) {
    val b = !s.isEmpty<caret>()
}