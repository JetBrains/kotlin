// "Replace with safe (?.) call" "false"
// ERROR: Infix call corresponds to a dot-qualified call 'p1.compareTo(p2)' which is not allowed on a nullable receiver 'p1'. Use '?.'-qualified call instead
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '>'
// ACTION: Replace overloaded operator with function call

class SafeType {
    operator fun compareTo(other : SafeType) = 0
}
fun safeA(p1: SafeType?, p2: SafeType) {
    val v8 = p1 <caret>> p2
}