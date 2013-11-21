// "Add parameter to function 'equals'" "false"
// ERROR: Too many arguments for public fun jet.Any?.equals(other: jet.Any?): jet.Boolean defined in jet

fun f(d: Any) {
    d.equals("a", <caret>"b")
}