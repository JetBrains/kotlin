// "Add non-null asserted (!!) call" "false"
// ACTION: Add 'i =' to argument
// ACTION: Change parameter 'i' type of function 'other' to 'Int?'
// ACTION: Do not show hints for current method
// ERROR: Null can not be a value of a non-null type Int
fun test() {
    other(<caret>null)
}

fun other(i: Int) {}