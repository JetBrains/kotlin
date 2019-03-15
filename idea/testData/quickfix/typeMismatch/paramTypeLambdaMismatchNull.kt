// "Surround with lambda" "false"
// ERROR: Null can not be a value of a non-null type () -> String
// ACTION: Change parameter 'block' type of function 'str' to '(() -> String)?'
// ACTION: Do not show hints for current method
fun nullFn() {
    str(<caret>null)
}

fun str(block: () -> String) {}