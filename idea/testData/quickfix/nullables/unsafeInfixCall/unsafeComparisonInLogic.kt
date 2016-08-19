// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '>'
// ACTION: Replace overloaded operator with function call
// ACTION: Simplify boolean expression
// ERROR: Infix call corresponds to a dot-qualified call 'w?.x.compareTo(42)' which is not allowed on a nullable receiver 'w?.x'. Use '?.'-qualified call instead

class Wrapper(val x: Int)

fun test(w: Wrapper?) {
    val t = 1 < 2 && w?.x <caret>> 42
}