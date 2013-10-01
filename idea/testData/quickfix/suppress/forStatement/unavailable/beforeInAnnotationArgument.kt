// "Suppress 'UNNECESSARY_NOT_NULL_ASSERTION' for statement " "false"
// ACTION: Remove unnecessary non-null assertion (!!)

[suppress("FOO"<caret>!!)]
fun foo() {}