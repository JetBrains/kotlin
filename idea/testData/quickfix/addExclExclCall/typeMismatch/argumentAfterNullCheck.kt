// "Add non-null asserted (!!) call" "false"
// ACTION: Add 'i =' to argument
// ACTION: Change parameter 'i' type of function 'other' to 'Int?'
// ACTION: Create function 'other'
// ACTION: Remove braces from 'if' statement
// ACTION: Surround with null check
// ACTION: Wrap with '?.let { ... }' call
// ERROR: Type mismatch: inferred type is Nothing? but Int was expected
fun test(i: Int?) {
    if (i == null) {
        other(<caret>i)
    }
}

fun other(i: Int) {}