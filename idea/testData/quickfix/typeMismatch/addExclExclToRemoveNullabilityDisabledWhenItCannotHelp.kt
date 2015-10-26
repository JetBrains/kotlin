// "Add non-null asserted (!!) call" "false"
// ACTION: Change parameter 's' type of function 'other' to 'String?'
// ERROR: <html>ype mismatch.<table><tr><td>Required:</td><td>kotlin.Int</td></tr><tr><td>Found:</td><td>kotlin.String?</td></tr></table></html>
fun test() {
    val s: String? = ""
    other(<caret>s)
}

fun other(s: Int) {}