// "Add parameter to function 'equals'" "false"
// ERROR: Too many arguments for public open fun equals(p0: jet.Any?): jet.Boolean defined in java.lang.Object

fun f(d: java.lang.Object) {
    d.equals("a", <caret>"b")
}