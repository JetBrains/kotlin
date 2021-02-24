// "Add 'return' before the expression" "false"

fun test(): Nothing {
    <caret>throw Throwable("Error")
}
