// "Add 'return' to last expression" "false"

fun test(): Nothing {
    throw RuntimeException("test")
}<caret>
