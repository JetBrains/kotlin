// "Change type of 'x' to 'String?'" "false"
// ACTION: Remove braces from 'if' statement
// ACTION: To raw string literal
// ACTION: Convert assignment to assignment expression
// ERROR: Type mismatch: inferred type is String but Nothing? was expected
// ERROR: Val cannot be reassigned
fun foo(condition: Boolean) {
    val x = null
    if (condition) {
        x = "abc"<caret>
    }
}