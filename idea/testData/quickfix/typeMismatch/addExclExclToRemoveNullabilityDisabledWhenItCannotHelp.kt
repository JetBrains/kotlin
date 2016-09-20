// "class org.jetbrains.kotlin.idea.quickfix.AddExclExclCallFix" "false"
// ACTION: Change parameter 's' type of function 'other' to 'String?'
// ACTION: Create function 'other'
// ERROR: Type mismatch: inferred type is String? but Int was expected
fun test() {
    val s: String? = ""
    other(<caret>s)
}

fun other(s: Int) {}