// "Wrap with '?.let { ... }' call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// ERROR: Operator call corresponds to a dot-qualified call 'l.contains(s)' which is not allowed on a nullable receiver 'l'.

fun test(l: List<String>?, s: String) {
    if (s <caret>in l) {}
}
