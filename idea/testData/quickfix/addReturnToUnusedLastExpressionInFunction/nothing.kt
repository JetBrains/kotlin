// "Add 'return' before the expression" "false"
// ACTION: Add '@Throws' annotation

fun test(): Nothing {
    <caret>throw Throwable("Error")
}
