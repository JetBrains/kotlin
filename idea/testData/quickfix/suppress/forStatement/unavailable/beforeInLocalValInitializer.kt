// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "false"
// ACTION: Disable 'Split Property Declaration'
// ACTION: Edit intention settings
// ACTION: Remove unnecessary non-null assertion (!!)
// ACTION: Split property declaration

fun foo() {
    val bar = ""<caret>!!
}