// PROBLEM: Replace negated 'isNotEmpty' with 'isEmpty'
// FIX: Replace negated 'isNotEmpty' with 'isEmpty'
// WITH_RUNTIME
fun test(s: String) {
    val b = !s.isNotEmpty<caret>()
}