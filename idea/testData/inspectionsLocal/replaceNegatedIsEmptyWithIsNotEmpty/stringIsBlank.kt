// PROBLEM: Replace negated 'isBlank' with 'isNotBlank'
// FIX: Replace negated 'isBlank' with 'isNotBlank'
// WITH_RUNTIME
fun test(s: String) {
    val b = !s.isBlank<caret>()
}