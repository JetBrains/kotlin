// "Add non-null asserted (!!) call" "false"
// ACTION: Change parameter 's' type of function 'other' to 'String?'
// ERROR: Type mismatch: inferred type is String? but Int was expected
fun test() {
    val s: String? = ""
    other(<caret>s)
}

fun other(s: Int) {}