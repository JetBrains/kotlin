// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "true"
// ERROR: Assignments are not expressions, and only expressions are allowed in this context

fun foo() {
    var x = 0
    x = 1<caret>!!
}