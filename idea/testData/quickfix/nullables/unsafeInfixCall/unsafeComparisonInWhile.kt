// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '>='
// ACTION: Replace overloaded operator with function call
// ERROR: Operator call corresponds to a dot-qualified call 'w?.x.compareTo(42)' which is not allowed on a nullable receiver 'w?.x'.

class Wrapper(val x: Int)

fun test(w: Wrapper?) {
    while (w?.x <caret>>= 42) {}
}