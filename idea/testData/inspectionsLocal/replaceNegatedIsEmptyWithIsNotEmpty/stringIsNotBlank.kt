// PROBLEM: Replace negated 'isNotBlank' with 'isBlank'
// FIX: Replace negated 'isNotBlank' with 'isBlank'
// WITH_RUNTIME
fun test(s: String) {
    val b = !s.isNotBlank<caret>()
}