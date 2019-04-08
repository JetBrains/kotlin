// "Round using roundToInt()" "false"
// DISABLE-ERRORS
// ACTION: Change parameter 'x' type of function 'foo' to 'Double'
// ACTION: Convert expression to 'Float'
// ACTION: Create function 'foo'
// WITH_RUNTIME
fun test(d: Double) {
    foo(d<caret>)
}

fun foo(x: Float) {}